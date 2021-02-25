package eu.interconnectproject.knowledge_engine.admin;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.AskResult;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

/**
 * Knowledge Base that regularly prints an overview of the currently available
 * Knowledge Bases within the network.
 *
 * We use Apache Jena's API extensively to work with the results of our ask.
 *
 */
public class AdminUI implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(AdminUI.class);

	private static final int SLEEPTIME = 2;
	private final SmartConnector sc;
	private final PrefixMapping prefixes;
	private volatile boolean connected = false;
	private ScheduledFuture<?> future;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private AskKnowledgeInteraction aKI;

	/**
	 * Intialize a AdminUI that regularly retrieves and prints metadata about the
	 * available knowledge bases.
	 */
	public AdminUI() {
		// store some predefined prefixes
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		// create a new Smart Connector for this Admin UI
		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();

		// we wait for the Smart Connector to be ready, before registering our Knowledge
		// Interactions and starting the Ask job.
	}

	@Override
	public URI getKnowledgeBaseId() {
		try {
			return new URI("https://www.tno.nl/energie/interconnect/adminui");
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
		return "Prints an overview of all the available Knowledge Bases to the console every few seconds.";
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {

		LOG.info("Smart connector ready, now registering Knowledge Interactions.");

		// first define your graph pattern
		GraphPattern gp = new GraphPattern(this.prefixes,
			"?kb rdf:type kb:KnowledgeBase .",
			"?kb kb:hasName ?name .",
			"?kb kb:hasDescription ?description .",
			"?kb kb:hasKnowledgeInteraction ?ki .",
			"?ki rdf:type ?kiType .",
			"?ki kb:isMeta ?isMeta .",
			"?ki kb:hasCommunicativeAct ?act .",
			"?act rdf:type kb:CommunicativeAct .",
			"?act kb:hasRequirement ?req .",
			"?act kb:hasSatisfaction ?sat .",
			"?req rdf:type ?reqType .",
			"?sat rdf:type ?satType .",
			"?ki kb:hasGraphPattern ?gp .",
			"?ki ?patternType ?gp .",
			"?gp rdf:type kb:GraphPattern .",
			"?gp kb:hasPattern ?pattern ."
		);

		// create the correct Knowledge Interaction
		this.aKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp);

		// register the knowledge interaction with the smart connector.
		this.sc.register(this.aKI);

		this.connected = true;

		// job that regularly retrieves and prints all available knowledge bases.
		this.future = this.executorService.scheduleWithFixedDelay(new AskRunnable(), 0, SLEEPTIME, TimeUnit.SECONDS);
	}

	/**
	 * The actual job (runnable) that retrieves and prints the available Knowledge
	 * Bases.
	 *
	 * @author nouwtb
	 *
	 */
	class AskRunnable implements Runnable {

		@Override
		public void run() {
			try {
				if (AdminUI.this.connected) {
					LOG.info("Retrieving other Knowledge Base info...");

					// execute actual *ask* and use previously defined Knowledge Interaction
					CompletableFuture<AskResult> askFuture = AdminUI.this.sc.ask(AdminUI.this.aKI, new BindingSet());

					LOG.info("test");
					// when result available, we print the knowledge bases to the console.
					askFuture.thenAccept(askResult -> {
						try {
							this.printKnowledgeBases(askResult);
						} catch (Throwable e) {
							LOG.error("{}", e);
						}
					});
				}
			} catch (Throwable t) {
				LOG.error("An error occurred while running the job.", t);
			}

		}

		private void printKnowledgeBases(AskResult askResult) throws ParseException {

			// using the BindingSet#generateModel() helper method, we can combine the graph
			// pattern and the bindings for its variables into a valid RDF Model.
			Model model = BindingSet.generateModel(AdminUI.this.aKI.getPattern(), askResult.getBindings());
			model.setNsPrefixes(AdminUI.this.prefixes);

//			LOG.info("{}", this.getRDF(model));

			LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
			LOG.info("-=-=-=-=-=-=-= KE Admin -=-=-=-=-=-=-=-");
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
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.info("Our Smart Connector lost its connection with the Knowledge Network.");
		this.connected = false;

	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {

		this.connected = true;
		LOG.info("Our Smart Connector restored its connection with the Knowledge Network.");

	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		LOG.info("Our Smart Connector has been succesfully stopped.");
	}

	private String getRDF(Model model) {
		StringWriter sw = new StringWriter();
		model.write(sw, "turtle");
		return sw.toString();
	}

	public void close() {
		this.sc.stop();
		this.future.cancel(true);
	}

}
