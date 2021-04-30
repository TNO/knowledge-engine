package eu.interconnectproject.knowledge_engine.examples.keo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.sse.SSE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

public class PowerUI implements KnowledgeBase {
	private static final Logger LOG = LoggerFactory.getLogger(PowerUI.class);

	private PrefixMappingMem prefixes;
	private URI knowledgeBaseId;

	private ReactKnowledgeInteraction rkiPower;
	private PostKnowledgeInteraction pkiPowerLimit;

	private SmartConnector sc;

	private static final String EX_DATA = "https://www.interconnectproject.eu/knowledge-engine/data/example/keo/";

	public PowerUI() throws URISyntaxException {
		this.prefixes = new PrefixMappingMem();
		this.prefixes.setNsPrefixes(PrefixMapping.Standard);
		this.prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		this.prefixes.setNsPrefix("om", "http://www.ontology-of-units-of-measure.org/resource/om-2/");
		this.prefixes.setNsPrefix("sosa", "http://www.w3.org/ns/sosa/");
		this.prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");
		this.prefixes.setNsPrefix("interconnect", "http://ontology.tno.nl/Interconnect#");
		this.prefixes.setNsPrefix("ex-data", EX_DATA);

		this.knowledgeBaseId = new URI(
				"https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/power-ui");

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

		// This KI listens for power measurements published on the network.
		this.rkiPower = new ReactKnowledgeInteraction(new CommunicativeAct(),
				new GraphPattern(this.prefixes, "?observation rdf:type sosa:Observation .",
						"?observation sosa:madeBySensor ?sensor .", "?observation sosa:observedProperty saref:Power .",
						"?observation sosa:hasResult ?result .", "?observation sosa:resultTime ?time .",
						"?result om:hasNumericalValue ?value .", "?result om:hasUnit om:watt ."),
				null
		);
		
		// When receiving such a power measurement, do some business logic and post
		// a power limit to the network.
		this.sc.register(this.rkiPower, (rki, bindings) -> {
			var binding = bindings.iterator().next();
			var value = binding.get("value");
			
			var n = (Float) SSE.parseNode(value).getLiteralValue();
			LOG.info("The power was {} at {}", n, binding.get("time"));

			BindingSet newBindings = new BindingSet();
			Binding powerLimitBinding = new Binding();

			// TODO: These should be unique.
			powerLimitBinding.put("limit", "<http://www.example.org/some-limit-object>");
			powerLimitBinding.put("command", "<http://www.example.org/some-command-object>");
			
			if (n > 500) {
				powerLimitBinding.put("limitValue", "\"500\"^^<http://www.w3.org/2001/XMLSchema#float>");
			} else {
				powerLimitBinding.put("limitValue", "\"100\"^^<http://www.w3.org/2001/XMLSchema#float>");
			}
			newBindings.add(powerLimitBinding);

			this.sc.post(this.pkiPowerLimit, newBindings);

			return new BindingSet();
		});

		// This KI allows to post power limit commands (actuations).
		this.pkiPowerLimit = new PostKnowledgeInteraction(
			new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.ACTUATION_PURPOSE)), new HashSet<>(Arrays.asList(Vocab.PURPOSE))),
			new GraphPattern(this.prefixes,
				"?limit om:hasUnit om:watt .",
				"?command rdf:type saref:SetLevelCommand .",
				"?command saref:actsUpon saref:PowerLimit .",
				"?limit om:hasNumericalValue ?limitValue .",
				"?command interconnect:SetsValue ?limit ."
			),
			null
		);
		this.sc.register(this.pkiPowerLimit);
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
