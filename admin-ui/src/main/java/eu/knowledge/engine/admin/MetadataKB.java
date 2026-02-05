package eu.knowledge.engine.admin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.javacc.ParseException;
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
import eu.knowledge.engine.smartconnector.api.MatchStrategy;
import eu.knowledge.engine.smartconnector.api.ReactExchangeInfo;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

/**
 * Knowledge Base that keeps track of all other KBs in the network. It does this
 * by subscribing to meta knowledge interactions and updating its state
 * accordingly.
 *
 * We use Apache Jena's API extensively to work with the results of our
 * interactions.
 *
 */
public class MetadataKB extends KnowledgeBaseImpl {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataKB.class);

	private static final String META_GRAPH_PATTERN_STR = "?kb rdf:type ke:KnowledgeBase . ?kb ke:hasName ?name . ?kb ke:hasDescription ?description . ?kb ke:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki ke:isMeta ?isMeta . ?ki ke:hasCommunicativeAct ?act . ?act rdf:type ke:CommunicativeAct . ?act ke:hasRequirement ?req . ?act ke:hasSatisfaction ?sat . ?req rdf:type ?reqType . ?sat rdf:type ?satType . ?ki ke:hasGraphPattern ?gp . ?gp rdf:type ?patternType . ?gp ke:hasPattern ?pattern .";

	private final PrefixMapping prefixes;

	// used for getting initial knowledge about other KBs
	private AskKnowledgeInteraction aKI;
	// used triggered when new knowledge about other KBs is available
	private ReactKnowledgeInteraction rKINew;
	// used triggered when knowledge about other KBs changed
	private ReactKnowledgeInteraction rKIChanged;
	// used triggered when knowledge about other KBs is deleted
	private ReactKnowledgeInteraction rKIRemoved;

	private Model metadata;

	private GraphPattern metaGraphPattern;

	private boolean timeToSleepAndFetch = true;

	/**
	 * Intialize a MetadataKB that collects metadata about the available knowledge
	 * bases.
	 */
	public MetadataKB(String id, String name, String description) {
		super(id, name, description);

		// store some predefined prefixes
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("ke", Vocab.ONTO_URI);

		this.metaGraphPattern = new GraphPattern(this.prefixes, META_GRAPH_PATTERN_STR);

		// create the correct Knowledge Interactions
		this.aKI = new AskKnowledgeInteraction(new CommunicativeAct(), this.metaGraphPattern, "ask-metadata", false,
				true, false, MatchStrategy.ENTRY_LEVEL);
		this.rKINew = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.NEW_KNOWLEDGE_PURPOSE)),
						new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null, "react-to-new-metadata");
		this.rKIChanged = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.CHANGED_KNOWLEDGE_PURPOSE)),
						new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null, "react-to-changed-metadata");
		this.rKIRemoved = new ReactKnowledgeInteraction(
				new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.REMOVED_KNOWLEDGE_PURPOSE)),
						new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE))),
				this.metaGraphPattern, null, "react-to-removed-metadata");

		// register the knowledge interactions with the smart connector.
		this.register(this.aKI);
		this.register(this.rKINew, (rki, ei) -> this.handleNewKnowledgeBase(ei));
		this.register(this.rKIChanged, (rki, ei) -> this.handleChangedKnowledgeBase(ei));
		this.register(this.rKIRemoved, (rki, ei) -> this.handleRemovedKnowledgeBase(ei));

	}

	@Override
	public void syncKIs() {
		super.syncKIs();

		if (timeToSleepAndFetch) {
			// to receive the initial state, we do a single Ask (after sleeping for a
			// specific amount of time)
			try {
				Thread.sleep(ConfigProvider.getConfig().getValue(AdminUIConfig.CONF_KEY_INITIAL_ADMIN_UI_DELAY,
						Integer.class));
			} catch (InterruptedException e) {
				LOG.error("Initial metadata KB delay should not fail.", e);
			}
			this.fetchInitialData();
			this.timeToSleepAndFetch = false;
		}
	}

	public BindingSet handleNewKnowledgeBase(ReactExchangeInfo ei) {
		if (!this.canReceiveUpdates())
			return new BindingSet();

		try {
			Model model = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
					eu.knowledge.engine.smartconnector.impl.Util.translateFromApiBindingSet(ei.getArgumentBindings()));

			Resource kb = model.listSubjectsWithProperty(RDF.type, Vocab.KNOWLEDGE_BASE).next();
			this.metadata.add(model);
			model.close();
			LOG.debug("Modified metadata with new KB '{}'.", kb);
		} catch (ParseException e) {
			LOG.error("{}", e);
		}
		return new BindingSet();
	}

	public BindingSet handleChangedKnowledgeBase(ReactExchangeInfo ei) {

		if (!this.canReceiveUpdates())
			return new BindingSet();

		try {
			Model model = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
					eu.knowledge.engine.smartconnector.impl.Util.translateFromApiBindingSet(ei.getArgumentBindings()));

			// this is a little more complex... we have to:
			// - extract the knowledge base that this message is about
			// - delete all old data about that knowledge base
			// - insert the *new* data about that knowledge base

			Resource kb = model.listSubjectsWithProperty(RDF.type, Vocab.KNOWLEDGE_BASE).next();
			String query = String.format("DELETE { %s } WHERE { %s FILTER (?kb = <%s>) } ",
					this.metaGraphPattern.getPattern(), this.metaGraphPattern.getPattern(), kb.toString());

			UpdateRequest updateRequest = UpdateFactory.create(query);
			UpdateAction.execute(updateRequest, this.metadata);

			this.metadata.add(model);

			model.close();

			LOG.debug("Modified metadata with changed KB '{}'.", kb);

		} catch (ParseException e) {
			LOG.error("{}", e);
		}
		return new BindingSet();
	}

	public BindingSet handleRemovedKnowledgeBase(ReactExchangeInfo ei) {
		if (!this.canReceiveUpdates())
			return new BindingSet();

		// this is also a little complex... we have to:
		// - extract the knowledge base that this message is about
		// - delete all old data about that knowledge base

		String kbUri = ei.getArgumentBindings().iterator().next().get("kb");
		Resource kb = this.metadata.createResource(kbUri.substring(1, kbUri.length() - 1));

		LOG.info("KB '{}'.", kb);

		String query = String.format("DELETE { %s } WHERE { %s FILTER (?kb = <%s>) } ",
				this.metaGraphPattern.getPattern(), this.metaGraphPattern.getPattern(), kb.toString());

		UpdateRequest updateRequest = UpdateFactory.create(query);
		UpdateAction.execute(updateRequest, this.metadata);

		LOG.debug("Modified metadata with deleted KB '{}'.", kb);

		return new BindingSet();
	}

	public void fetchInitialData() {
		LOG.info("Retrieving initial other Knowledge Base info...");

		try {

			// execute actual *ask* and use previously defined Knowledge Interaction.
			this.getSC().ask(this.aKI, new BindingSet()).thenAccept(askResult -> {
				try {

					if (this.metadata != null)
						this.metadata.close();

					// using the BindingSet#generateModel() helper method, we can combine the graph
					// pattern and the bindings for its variables into a valid RDF Model.
					this.metadata = eu.knowledge.engine.smartconnector.impl.Util.generateModel(this.aKI.getPattern(),
							eu.knowledge.engine.smartconnector.impl.Util
									.translateFromApiBindingSet(askResult.getBindings()));
					this.metadata.setNsPrefixes(this.prefixes);

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
			}).get();
		} catch (ExecutionException | InterruptedException ee) {
			LOG.error("{}", ee);
		}

	}

	protected boolean canReceiveUpdates() {
		return this.metadata != null;
	}

	public void close() {
		this.stop();
	}

	public Model getMetadata() {
		return metadata;
	}
}
