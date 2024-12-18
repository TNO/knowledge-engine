package eu.knowledge.engine.admin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

/**
 * Knowledge Base that regularly prints an overview of the currently available
 * Knowledge Bases within the network.
 *
 * We use Apache Jena's API extensively to work with the results of our ask.
 *
 */
public class AdminUI implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(AdminUI.class);

	private static final String META_GRAPH_PATTERN_STR = "?kb rdf:type ke:KnowledgeBase . ?kb ke:hasName ?name . ?kb ke:hasDescription ?description . ?kb ke:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki ke:isMeta ?isMeta . ?ki ke:hasCommunicativeAct ?act . ?act rdf:type ke:CommunicativeAct . ?act ke:hasRequirement ?req . ?act ke:hasSatisfaction ?sat . ?req rdf:type ?reqType . ?sat rdf:type ?satType . ?ki ke:hasGraphPattern ?gp . ?gp rdf:type ?patternType . ?gp ke:hasPattern ?pattern .";

	private SmartConnector sc;
	private final PrefixMapping prefixes;

	// used for getting initial knowledge about other KBs
	private AskKnowledgeInteraction aKI;
	// used triggered when new knowledge about other KBs is available
	private ReactKnowledgeInteraction rKINew;
	// used triggered when knowledge about other KBs changed
	private ReactKnowledgeInteraction rKIChanged;
	// used triggered when knowledge about other KBs is deleted
	private ReactKnowledgeInteraction rKIRemoved;

	private static AdminUI instance;
	private static boolean continuousLog = true;
	private static String knowledgeBaseId = "https://knowledge-engine.eu/adminui-" + UUID.randomUUID();

	private Model model;

	private GraphPattern metaGraphPattern;

	/**
	 * Intialize a AdminUI that regularly retrieves and prints metadata about the
	 * available knowledge bases.
	 */
	private AdminUI() {
		// store some predefined prefixes
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("ke", Vocab.ONTO_URI);

		this.metaGraphPattern = new GraphPattern(this.prefixes, META_GRAPH_PATTERN_STR);
		// create a new Smart Connector for this Admin UI
		SmartConnectorBuilder.newSmartConnector(this).create();

		// we wait for the Smart Connector to be ready, before registering our Knowledge
		// Interactions and starting the Ask job.
	}

	public static AdminUI newInstance(boolean useLog) {
		if (instance == null) {
			continuousLog = useLog;
			instance = new AdminUI();
		}
		return instance;
	}

	@Override
	public URI getKnowledgeBaseId() {
		try {
			return new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			LOG.error("{}", e);
			return null;
		}
	}

	@Override
	public String getKnowledgeBaseName() {
		return "Admin UI";
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return "Publishes an overview of all the available Knowledge Bases via a REST API.";
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		this.sc = aSC;

		LOG.info("Smart connector ready, now registering Knowledge Interactions.");

		// create the correct Knowledge Interactions
		this.aKI = new AskKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern, true);
		this.rKINew = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE)),
						new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null);
		this.rKIChanged = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE)),
						new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null);
		this.rKIRemoved = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE)),
						new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null);

		// register the knowledge interactions with the smart connector.
		this.sc.register(this.aKI);
		this.sc.register(this.rKINew, (rki, ei) -> this.handleNewKnowledgeBaseKnowledge(ei));
		this.sc.register(this.rKIChanged, (rki, ei) -> this.handleChangedKnowledgeBaseKnowledge(ei));
		this.sc.register(this.rKIRemoved, (rki, ei) -> this.handleRemovedKnowledgeBaseKnowledge(ei));

		// to receive the initial state, we do a single Ask (after sleeping for a
		// specific amount of time)
		try {
			Thread.sleep(
					ConfigProvider.getConfig().getValue(AdminUIConfig.CONF_KEY_INITIAL_ADMIN_UI_DELAY, Integer.class));
		} catch (InterruptedException e) {
			LOG.info("{}", e);
		}
		this.fetchInitialData();
	}

	public BindingSet handleNewKnowledgeBaseKnowledge(ReactExchangeInfo ei) {
		if (!this.canReceiveUpdates())
			return new BindingSet();

		try {
			Model model = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
					ei.getArgumentBindings());

			// this we can simply add to our model
			this.model.add(model);

			// when result available (and the config is enabled), we print the
			// knowledge bases to the console.
			if (continuousLog) {
				this.printKnowledgeBases(this.model);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new BindingSet();
	}

	public BindingSet handleChangedKnowledgeBaseKnowledge(ReactExchangeInfo ei) {
		if (!this.canReceiveUpdates())
			return new BindingSet();

		try {
			Model model = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
					ei.getArgumentBindings());

			// this is a little more complex... we have to:
			// - extract the knowledge base that this message is about
			// - delete all old data about that knowledge base
			// - insert the *new* data about that knowledge base

			Resource kb = model.listSubjectsWithProperty(RDF.type, Vocab.KNOWLEDGE_BASE).next();
			String query = String.format("DELETE { %s } WHERE { %s FILTER (?kb = <%s>) } ",
					this.metaGraphPattern.getPattern(), this.metaGraphPattern.getPattern(), kb.toString());

			UpdateRequest updateRequest = UpdateFactory.create(query);
			UpdateAction.execute(updateRequest, this.model);

			this.model.add(model);

			// when result available (and the config is enabled), we print the
			// knowledge bases to the console.
			if (continuousLog) {
				this.printKnowledgeBases(this.model);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new BindingSet();
	}

	public BindingSet handleRemovedKnowledgeBaseKnowledge(ReactExchangeInfo ei) {
		if (!this.canReceiveUpdates())
			return new BindingSet();

		try {
			Model model = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
					ei.getArgumentBindings());

			// this is also a little complex... we have to:
			// - extract the knowledge base that this message is about
			// - delete all old data about that knowledge base

			Resource kb = model.listSubjectsWithProperty(RDF.type, Vocab.KNOWLEDGE_BASE).next();
			String query = String.format("DELETE { %s } WHERE { %s FILTER (?kb = <%s>) } ",
					this.metaGraphPattern.getPattern(), this.metaGraphPattern.getPattern(), kb.toString());

			UpdateRequest updateRequest = UpdateFactory.create(query);
			UpdateAction.execute(updateRequest, this.model);

			// when result available (and the config is enabled), we print the
			// knowledge bases to the console.
			if (continuousLog) {
				this.printKnowledgeBases(this.model);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new BindingSet();
	}

	public void fetchInitialData() {
		LOG.info("Retrieving initial other Knowledge Base info...");

		// execute actual *ask* and use previously defined Knowledge Interaction.
		this.sc.ask(this.aKI, new BindingSet()).thenAccept(askResult -> {
			try {
				// using the BindingSet#generateModel() helper method, we can combine the graph
				// pattern and the bindings for its variables into a valid RDF Model.
				this.model = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
						askResult.getBindings());
				this.model.setNsPrefixes(this.prefixes);
				// when result available (and the config is enabled), we print the
				// knowledge bases to the console.
				if (continuousLog)
					this.printKnowledgeBases(this.model);
			} catch (ParseException e) {
				LOG.error("{}", e);
			}
		}).handle((r, e) -> {
			if (r == null && e != null) {
				LOG.error("An exception has occured while retrieving other Knowledge Bases info", e);
				return null;
			} else {
				return r;
			}
		});
	}

	private boolean canReceiveUpdates() {
		return this.model != null;
	}

	private void printKnowledgeBases(Model model) throws ParseException {

		// LOG.info("{}", this.getRDF(model));

		LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		LOG.info("-=-=-=-=-=-=-= Admin UI -=-=-=-=-=-=-=-");
		LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		if (!model.isEmpty()) {
			Set<Resource> kbs = Util.getKnowledgeBaseURIs(model);

			int i = 0;
			for (Resource kbRes : kbs) {
				i++;

				if (i > 1) {
					LOG.info("");
				}

				LOG.info("Knowledge Base <{}>", kbRes);

				LOG.info("\t* Name: {}", Util.getName(model, kbRes));
				LOG.info("\t* Description: {}", Util.getDescription(model, kbRes));

				Set<Resource> kiResources = Util.getKnowledgeInteractionURIs(model, kbRes);

				for (Resource kiRes : kiResources) {
					String knowledgeInteractionType = Util.getKnowledgeInteractionType(model, kiRes);
					LOG.info("\t* {}{}", knowledgeInteractionType, (Util.isMeta(model, kiRes) ? " (meta)" : ""));
					if (knowledgeInteractionType.equals("AskKnowledgeInteraction")
							|| knowledgeInteractionType.equals("AnswerKnowledgeInteraction")) {
						LOG.info("\t\t- GraphPattern: {}", Util.getGraphPattern(model, kiRes));
					} else if (knowledgeInteractionType.equals("PostKnowledgeInteraction")
							|| knowledgeInteractionType.equals("ReactKnowledgeInteraction")) {
						LOG.info("\t\t- Argument GP: {}", Util.getArgument(model, kiRes));
						LOG.info("\t\t- Result GP: {}", Util.getResult(model, kiRes));
					}
				}
			}
		} else {
			LOG.info("No other knowledge bases found.");
		}
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.info("Our Smart Connector lost its connection with the Knowledge Network.");
	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		LOG.info("Our Smart Connector restored its connection with the Knowledge Network.");
	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		LOG.info("Our Smart Connector has been succesfully stopped.");
	}

	public void close() {
		this.sc.stop();
	}

	public Model getModel() {
		return model;
	}
}
