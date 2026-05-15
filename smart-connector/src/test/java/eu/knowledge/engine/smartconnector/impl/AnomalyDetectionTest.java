/**
 * 
 */
package eu.knowledge.engine.smartconnector.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

/**
 * The anomaly detection example behaves strangely. If we add the anomaly logger
 * KB, the anomaly detector KB suddenly does not receive any measurements from
 * sensor1. Otherwise, it does receive them.
 */
class AnomalyDetectionTest {

	/**
	 * Log facility of this class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(AnomalyDetectionTest.class);
	private static KnowledgeNetwork kn;
	private static KnowledgeBaseImpl sensorKB;
	private static PostKnowledgeInteraction measurementPostKI;
	private static PrefixMappingMem prefixes = new PrefixMappingMem();

	@Test
	void test() throws InterruptedException, ExecutionException {
		prefixes.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixes.setNsPrefix("dct", "http://purl.org/dc/terms/");
		prefixes.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
		prefixes.setNsPrefix("ex", "http://example.org/");
		prefixes.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		kn = new KnowledgeNetwork();
		// anomaly detector
		createAnomalyDetectionKB();
		// anomaly logger
		createAnomalyLoggerKB();
		// sensor
		createSensorKB();
		kn.sync();
		LOG.info("Everyone up-to-date!");
		BindingSet bs = new BindingSet();
		var b = new Binding();
		b.put("m", "<http://example.org/sensor1/measurement1>");
		bs.add(b);
		var result = sensorKB.post(measurementPostKI, bs).get();
		assertFalse(result.getExchangeInfoPerKnowledgeBase().isEmpty());

	}

	private void createSensorKB() {
		sensorKB = new KnowledgeBaseImpl("sensor-kb");
		sensorKB.setReasonerLevel(2);
		var gp = new GraphPattern(prefixes, """
				        ?m rdf:type ex:Measurement .
				""");
		measurementPostKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null, "publish-measurement");
		sensorKB.register(measurementPostKI);
		var rules = """
				@prefix ex: <http://example.org/> .

				-> ( ex:DutchTemperatureSensor rdfs:subClassOf ex:Sensor ) .

				(?x rdfs:subClassOf ?y), (?a rdf:type ?x) -> (?a rdf:type ?y) .
				""";
		sensorKB.setDomainKnowledge(JenaRules.convertJenaToKeRules(rules).stream().map((BaseRule r) -> (Rule) r)
				.collect(Collectors.toSet()));
		kn.addKB(sensorKB);

	}

	private void createAnomalyLoggerKB() {
		var anomalyLoggerKB = new KnowledgeBaseImpl("anomaly-logger-kb");
		var gp = new GraphPattern(prefixes, """
				       ?report rdf:type sh:ValidationReport .
				""");
		var reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp, null, "subscribe-report");
		anomalyLoggerKB.register(reactKI, (ki, ei) -> {
			LOG.info("AnomalyLogger: Received anomaly report: {}",
					ei.getArgumentBindings().iterator().next().get("resultStatusMsgLong"));
			return new BindingSet();
		});
		kn.addKB(anomalyLoggerKB);

	}

	private void createAnomalyDetectionKB() {
		var anomalyDetectionKB = new KnowledgeBaseImpl("anomaly-detection-kb");
		// react KI
		var gp = new GraphPattern(prefixes, """
				        ?m rdf:type ex:Measurement .

				""");
		var reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp, null, "subscribe-measurement");
		anomalyDetectionKB.register(reactKI, (_, ei) -> {

			LOG.info("AnomalyDetector: Received measurement: {}", ei.getArgumentBindings().iterator().next().get("m"));
			return new BindingSet();
		});
		kn.addKB(anomalyDetectionKB);
	}

	@AfterAll
	public static void close() {
		kn.stop();
	}

}
