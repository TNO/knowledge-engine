package eu.interconnectproject.knowledge_engine.examples.keo;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

public class PowerUI implements KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(PowerUI.class);

	private PrefixMappingMem prefixes;
	private URI knowledgeBaseId;
	private ReactKnowledgeInteraction rkiPower;

	private SmartConnector sc;

	private static final String EX_DATA = "https://www.interconnectproject.eu/knowledge-engine/data/example/keo/";

	public PowerUI() throws URISyntaxException {
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.prefixes.setNsPrefix("om", "http://www.ontology-of-units-of-measure.org/resource/om-2/");
		this.prefixes.setNsPrefix("sosa", "http://www.w3.org/ns/sosa/");
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");
		this.prefixes.setNsPrefix("ex-data", EX_DATA);
		
		this.knowledgeBaseId = new URI("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/power-ui");

		this.sc = SmartConnectorBuilder.newSmartConnector(this).create();
	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.knowledgeBaseId;
	}

	@Override
	public String getKnowledgeBaseName() {
		return "Epic Power Visualization Knowledge Base";
	}

	@Override
	public String getKnowledgeBaseDescription() {
		return "This knowledge base visualizes power measurements in an epic manner.";
	}

	@Override
	public void smartConnectorReady(SmartConnector aSC) {
		LOG.info("Smart connector ready.");
		this.rkiPower = new ReactKnowledgeInteraction(
			new CommunicativeAct(),
			new GraphPattern(this.prefixes,
				"?observation rdf:type sosa:Observation .",
				"?observation sosa:madeBySensor ?sensor .",
				"?observation sosa:observedProperty saref:Power .",
				"?observation sosa:hasResult ?result .",
				"?observation sosa:resultTime ?time .",
				"?result om:hasNumericalValue ?value .",
				"?result om:hasUnit om:watt ."
			),
			null
		);
		aSC.register(this.rkiPower, (rki, bindings) -> {
			var binding = bindings.iterator().next();
			LOG.info("The power was {} at {}", binding.get("value"), binding.get("time"));
			return new BindingSet();
		});
	}

	@Override
	public void smartConnectorConnectionLost(SmartConnector aSC) {
		LOG.warn("Connection lost with smart connector.");
	}

	@Override
	public void smartConnectorConnectionRestored(SmartConnector aSC) {
		LOG.info("Connection with smart connector restored.");
	}

	@Override
	public void smartConnectorStopped(SmartConnector aSC) {
		LOG.info("Smart connector stopped.");
	}
}
