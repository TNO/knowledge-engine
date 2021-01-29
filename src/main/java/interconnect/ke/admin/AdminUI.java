package interconnect.ke.admin;

import java.net.URI;
import java.net.URISyntaxException;
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

public class AdminUI implements KnowledgeBase {

	private static final int SLEEPTIME = 2;

	private static final Logger LOG = LoggerFactory.getLogger(AdminUI.class);

	private final SmartConnector sc;
	private final PrefixMapping prefixes;
	private volatile boolean connected = false;
	private ScheduledFuture<?> future;

	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	private AskKnowledgeInteraction aKI;

	public static void main(String[] args) throws InterruptedException, BrokenBarrierException, TimeoutException {
		AdminUI ui = new AdminUI();
		ui.start();
		ui.close();
	}

	public AdminUI() {
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect#");
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();

	}

	public void start() throws InterruptedException, BrokenBarrierException, TimeoutException {
		LOG.info("Admin UI started.");

		this.future = this.executorService.scheduleWithFixedDelay(() -> {

			if (this.connected) {
				LOG.info("Retrieving other Knowledge Base info...");
				this.sc.ask(this.aKI, new BindingSet()).thenAccept(askResult -> {
					try {
						Model m = BindingSet.generateModel(this.aKI.getPattern(), askResult.getBindings());
						m.setNsPrefixes(this.prefixes);
						System.out.println("Printing current other knowledge base information:");

						if (!m.isEmpty()) {

							ResIterator iter = m.listResourcesWithProperty(RDF.type,
									m.createResource(this.prefixes.expandPrefix("kb:KnowledgeBase")));

							Resource r;
							while (iter.hasNext()) {
								r = iter.next();

								System.out.println("Knowledge Base found with id: " + r);

								System.out.println("\tName: "
										+ r.getProperty(m.createProperty(this.prefixes.expandPrefix("kb:hasName")))
												.getObject());
								System.out.println("\tDescription: " + r
										.getProperty(m.createProperty(this.prefixes.expandPrefix("kb:hasDescription")))
										.getObject());

								StmtIterator kiIter = m.listStatements(r,
										m.createProperty(this.prefixes.expandPrefix("kb:hasKnowledgeInteraction")),
										(RDFNode) null);

								int i = 0;
								while (kiIter.hasNext()) {
									kiIter.next();
									i++;
								}
								System.out.println("\tNr of Knowledge Iteractions: " + i);

							}

						} else {
							System.out.println("No other knowledge bases found.");
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

	public void close() {
		this.sc.stop();
		this.future.cancel(true);

	}
}
