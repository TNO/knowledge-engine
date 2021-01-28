package interconnect.ke.sc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;

public class AdminUI implements KnowledgeBase {

	private static final Logger LOG = LoggerFactory.getLogger(AdminUI.class);

	private final SmartConnector sc;
	private final PrefixMapping prefixes;
	private final CyclicBarrier connected;

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

		this.connected = new CyclicBarrier(1);
		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();

	}

	private void start() throws InterruptedException, BrokenBarrierException, TimeoutException {
		LOG.info("Admin UI started.");

		while (true) {

			try {
				this.connected.await(5, TimeUnit.SECONDS);

				// smart connector is ready and connected

				LOG.info("Before ask");
				this.sc.ask(this.aKI, new BindingSet()).thenAccept(askResult -> {
					try {
						Model m = BindingSet.generateModel(this.aKI.getPattern(), askResult.getBindings());
						this.clear();
						LOG.info("{}", m);

					} catch (Throwable e) {
						LOG.error("{}", e);
					}
				});
				LOG.info("After ask");

			} catch (TimeoutException te) {
				// do nothing

				this.connected.reset();

			}

		}

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
		return "Displays an overview of all the Knowledge Bases and their interactions in the knowledge network.";
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		GraphPattern gp = new GraphPattern(this.prefixes,
				"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasGraphPattern ?gp . ?ki ?patternType ?gp . ?gp rdf:type kb:GraphPattern . ?gp kb:hasPattern ?pattern .");
		this.aKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
		this.sc.register(this.aKI);
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		this.clear();
		LOG.info("Our Smart Connector lost its connection with the Knowledge Network.");
	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		this.clear();
		LOG.info("Our Smart Connector restored its connection with the Knowledge Network.");

	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		this.clear();
		LOG.info("Our Smart Connector has been succesfully stopped.");
	}

	private void close() {
		this.sc.stop();

	}

	private void clear() {
		try {
			final String os = System.getProperty("os.name");

			if (os.contains("Windows")) {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} else {
				new ProcessBuilder("clear").inheritIO().start().waitFor();
			}
		} catch (final Exception e) {
			// Handle any exceptions.
			LOG.error("{}", e);
		}
	}

}
