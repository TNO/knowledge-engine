package eu.knowledge.engine.smartconnector.api;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

/**
 * This unit tests tries to execute a decision tree using the reasoning
 * capabilities of the KE. The decision tree is encoded in domain knowledge (in
 * the form of domain rules) that get taken into account when asking or posting
 * data.
 * 
 * Story: A camera sensor publishes observations of the contrast/brightness of
 * the camera image, which is analysed by the an anomaly detector. When the
 * brightness suddenly goes down, the anomaly detector will produce an anomaly.
 * A dependency graph determines what the possible causes of the anomaly on the
 * camera image could be and comes up with the sensor itself or the weather.
 * Then this unit test starts with those two possible causes.
 * 
 * A diagnose KB asks the Knowledge Network for the probability that a few
 * systems are causing an anomaly on a dependent system. There are two
 * dependencies: sensor info and weather.
 * 
 * The sensor info KB tells whether a particular sensor is old or young.
 * 
 * The weather KB tells whether there is currently a sandstorm in the area.
 * 
 * The decision tree is simple: if there currently is a sandstorm, then the
 * weather probably of the weather being the possible cause is high and the
 * probability of the sensor being the possible cause is low, while if there is
 * no sandstorm, the probabilities are reversed.
 */
class TestDecisionTreeReasoning {

	private static final Logger LOG = LoggerFactory.getLogger(TestDecisionTreeReasoning.class);

	private KnowledgeNetwork network;
	private KnowledgeBaseImpl diagnoseKb;
	private KnowledgeBaseImpl sensorInfoKb;
	private KnowledgeBaseImpl weatherKb;
	private KnowledgeBaseImpl causesKb;

	private PrefixMapping pm = null;

	@Test
	void test() throws InterruptedException, ExecutionException {
		pm = new PrefixMappingMem();
		pm.setNsPrefixes(PrefixMapping.Standard);
		pm.setNsPrefix("ex", "https://www.example.org/");

		network = new KnowledgeNetwork();
		diagnoseKb = new KnowledgeBaseImpl("diagnoseKb");
		var askKI = configureDiagnoseKb();
		network.addKB(diagnoseKb);
		sensorInfoKb = new KnowledgeBaseImpl("sensorInfoKb");
		configureSensorInfoKb();
		network.addKB(this.sensorInfoKb);
		weatherKb = new KnowledgeBaseImpl("weatherKb");
		configureWeatherKb();
		network.addKB(weatherKb);
		causesKb = new KnowledgeBaseImpl("causesKb");
		configureCausesKb();
		network.addKB(causesKb);

		network.sync();

		var bindingSet = new BindingSet();
		var b = new Binding();
		b.put("s", "<https://www.example.org/weather>");
		bindingSet.add(b);
		b = new Binding();
		b.put("s", "<https://www.example.org/sensor>");
		bindingSet.add(b);

		AskResult ar = this.diagnoseKb.ask(askKI, bindingSet).get();
		LOG.info("Result: {}", ar);
	}

	private void configureCausesKb() {
		GraphPattern diagnoseGp2 = new GraphPattern(this.pm, """
				?s rdf:type ex:PossibleCause .
				""");
		AnswerKnowledgeInteraction answerKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), diagnoseGp2,
				"sensorInfoKI2");
		this.causesKb.register(answerKI2, (_, _) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("s", "<https://www.example.org/weather>");
			bs.add(b);
			b = new Binding();
			b.put("s", "<https://www.example.org/sensor>");
			bs.add(b);
			return bs;
		});
	}

	private void configureWeatherKb() {
		GraphPattern gp = new GraphPattern(this.pm, """
				?w ex:hasSandstorm ?b .
				""");

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp, "weatherKI");
		this.weatherKb.register(answerKI, (_, _) -> {

			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("w", "<https://www.example.org/weather>");
			b.put("b", "\"true\"");
			bs.add(b);
			return bs;
		});
	}

	private void configureSensorInfoKb() {
		GraphPattern gp = new GraphPattern(this.pm, """
				?sens ex:hasAge ?age .
				""");

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp,
				"sensorInfoKI");

		this.sensorInfoKb.register(answerKI, (_, _) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("sens", "<https://www.example.org/sensor>");
			b.put("age", "<https://www.example.org/old>");
			bs.add(b);
			b = new Binding();
			b.put("sens", "<https://www.example.org/otherSensor>");
			b.put("age", "<https://www.example.org/young>");
			return bs;
		});
	}

	private AskKnowledgeInteraction configureDiagnoseKb() {
		GraphPattern diagnoseGp = new GraphPattern(this.pm, """
				?s rdf:type ex:PossibleCause .
				?s ex:hasProbability ?p .
				""");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), diagnoseGp, "diagnoseKI");
		this.diagnoseKb.register(askKI);

		String rules = """

				@prefix ex: <https://www.example.org/>

				( ?s ex:hasSandstorm "true" ) -> ( ?s ex:hasProbability <https://www.example.org/high> ) .
				( ?s ex:hasSandstorm "false" ) -> ( ?s ex:hasProbability <https://www.example.org/low> ) .
				( ?s ex:hasAge <https://www.example.org/old> ) -> (?s ex:hasProbability <https://www.example.org/high> ) .
				( ?s ex:hasAge <https://www.example.org/young> ) -> (?s ex:hasProbability <https://www.example.org/low> ) .

				""";

		Set<BaseRule> someRules = JenaRules.convertJenaToKeRules(rules);
		Set<Rule> dkRules = new HashSet<Rule>();

		for (BaseRule br : someRules) {
			dkRules.add((Rule) br);
		}

		this.diagnoseKb.setReasonerLevel(3);
		this.diagnoseKb.setDomainKnowledge(dkRules);

		return askKI;
	}

}
