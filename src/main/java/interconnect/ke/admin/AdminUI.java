package interconnect.ke.admin;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.sc.SmartConnectorBuilder;
import interconnect.ke.sc.Vocab;

/**
 * Knowledge Base that regularly prints an overview of the currently available
 * Knowledge Bases within the network.
 *
 */
public class AdminUI implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(AdminUI.class);

	private static final String NONE = "<none>";
	private static final int SLEEPTIME = 2;
	private final SmartConnector sc;
	private final PrefixMapping prefixes;
	private volatile boolean connected = false;
	private ScheduledFuture<?> future;
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private AskKnowledgeInteraction aKI;

	public AdminUI() {
		// store some predefined prefixes
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
	}

	public void start() throws InterruptedException, BrokenBarrierException, TimeoutException {
		LOG.info("Admin UI started.");

		this.future = this.executorService.scheduleWithFixedDelay(() -> {

			if (this.connected) {
				LOG.debug("Retrieving other Knowledge Base info...");
				this.sc.ask(this.aKI, new BindingSet()).thenAccept(askResult -> {
					try {
						Model model = BindingSet.generateModel(this.aKI.getPattern(), askResult.getBindings());
						model.setNsPrefixes(this.prefixes);

//						LOG.info("{}", this.getRDF(model));

						LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
						LOG.info("-=-=-=-=-=-=-= KE Admin -=-=-=-=-=-=-=-");
						LOG.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
						if (!model.isEmpty()) {

							Set<Resource> kbs = this.getKnowledgeBaseURIs(model);

							int i = 0;
							for (Resource kbRes : kbs) {
								i++;

								if (i > 1) {
									LOG.info("");
								}

								LOG.info("Knowledge Base <{}>", kbRes);

								LOG.info("\t* Name: {}", this.getName(model, kbRes));
								LOG.info("\t* Description: {}", this.getDescription(model, kbRes));

								Set<Resource> kiResources = this.getKnowledgeInteractionURIs(model, kbRes);

								for (Resource kiRes : kiResources) {
									String knowledgeInteractionType = this.getKnowledgeInteractionType(model, kiRes);
									LOG.info("\t* {}{}", knowledgeInteractionType,
											(this.isMeta(model, kiRes) ? " (meta)" : ""));
									if (knowledgeInteractionType.equals("AskKnowledgeInteraction")
											|| knowledgeInteractionType.equals("AnswerKnowledgeInteraction")) {
										LOG.info("\t\t- GraphPattern: {}", this.getGraphPattern(model, kiRes));
									} else if (knowledgeInteractionType.equals("PostKnowledgeInteraction")
											|| knowledgeInteractionType.equals("ReactKnowledgeInteraction")) {
										LOG.info("\t\t- Argument GP: {}", this.getArgument(model, kiRes));
										LOG.info("\t\t- Result GP: {}", this.getResult(model, kiRes));
									}
								}
							}

						} else {
							LOG.info("No other knowledge bases found.");
						}

					} catch (Throwable e) {
						LOG.error("{}", e);
					}

				});
			}
		}, 0, SLEEPTIME, TimeUnit.SECONDS);
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
		return "Displays an overview of all the Knowledge Bases in the network and their interactions.";
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {

		LOG.info("Smart connector ready.");

		GraphPattern gp = new GraphPattern(this.prefixes,
				"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasGraphPattern ?gp . ?ki ?patternType ?gp . ?gp rdf:type kb:GraphPattern . ?gp kb:hasPattern ?pattern .");
		this.aKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
		this.sc.register(this.aKI);

		this.connected = true;

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

	private Set<Resource> getKnowledgeBaseURIs(Model m) {

		ResIterator iter = m.listResourcesWithProperty(RDF.type,
				m.createResource(this.prefixes.expandPrefix("kb:KnowledgeBase")));

		Set<Resource> kbs = new HashSet<>();

		while (iter.hasNext()) {
			kbs.add(iter.next());
		}
		return kbs;
	}

	private String getName(Model m, Resource r) {
		return this.getProperty(m, r, this.prefixes.expandPrefix("kb:hasName"));
	}

	private String getDescription(Model m, Resource r) {
		return this.getProperty(m, r, this.prefixes.expandPrefix("kb:hasDescription"));
	}

	private Set<Resource> getKnowledgeInteractionURIs(Model m, Resource r) {
		StmtIterator kiIter = m.listStatements(r,
				m.getProperty(this.prefixes.expandPrefix("kb:hasKnowledgeInteraction")), (RDFNode) null);

		Set<Resource> kis = new HashSet<>();

		while (kiIter.hasNext()) {
			kis.add(kiIter.next().getObject().asResource());
		}
		return kis;
	}

	private String getKnowledgeInteractionType(Model m, Resource r) {
		return r.getPropertyResourceValue(RDF.type).getLocalName();
	}

	private boolean isMeta(Model model, Resource kiRes) {

		return kiRes.getProperty(model.createProperty(this.prefixes.expandPrefix("kb:isMeta"))).getObject().asLiteral()
				.getBoolean();

	}

	private String getGraphPattern(Model model, Resource kiRes) {
		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(this.prefixes.expandPrefix("kb:hasGraphPattern")));
		return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().toString();

	}

	private String getArgument(Model model, Resource kiRes) {
		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(this.prefixes.expandPrefix("kb:hasArgumentGraphPattern")));
		if (gpRes != null) {
			return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().toString();
		} else {
			return NONE;
		}
	}

	private String getResult(Model model, Resource kiRes) {

		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(this.prefixes.expandPrefix("kb:hasResultGraphPattern")));
		if (gpRes != null) {
			return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().toString();
		} else {
			return NONE;
		}
	}

	private String getProperty(Model m, Resource r, String propertyURI) {
		return r.getProperty(m.getProperty(propertyURI)).getObject().toString();
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
