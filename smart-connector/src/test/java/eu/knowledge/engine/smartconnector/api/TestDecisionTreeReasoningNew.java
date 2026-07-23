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
 * the camera image, which is analysed by the anomaly detector. When the
 * brightness suddenly goes down, the anomaly detector will produce an anomaly.
 * A dependency graph determines what the possible components that can cause
 * the anomaly on the camera image could be and comes up with the 
 * light sensor, the lens or the battery. 
 * 
 * Then this unit test starts with these possible causes. There are multiple
 * rules for deriving the more detailed cause of the anomaly, such as weather
 * situation, period of the day or usage history of the component.
 * 
 * Therefore, a weather KB and a maintenance KB will be setup to provide extra
 * information to reason over.
 * 
 * 
 * 
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
 * 
 * TODO: update this description according to the new scenario !!
 */
class TestDecisionTreeReasoningNew {

	private static final Logger LOG = LoggerFactory.getLogger(TestDecisionTreeReasoningNew.class);

	private KnowledgeNetwork network;
	private KnowledgeBaseImpl diagnoseKb;
	private KnowledgeBaseImpl systemInfoKb;
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
		systemInfoKb = new KnowledgeBaseImpl("systemInfoKb");
		configureSystemInfoKb();
		network.addKB(systemInfoKb);
		weatherKb = new KnowledgeBaseImpl("weatherKb");
		configureWeatherKb();
		network.addKB(weatherKb);
		causesKb = new KnowledgeBaseImpl("causesKb");
		configureCausesKb();
		network.addKB(causesKb);

		network.sync();

		var bindingSet = new BindingSet();
		var b = new Binding();
		b.put("system", "<https://www.example.org/Camera>");
		b.put("anomaly", "<https://www.example.org/LowVideoQuality>");
		bindingSet.add(b);

		AskResult ar = this.diagnoseKb.ask(askKI, bindingSet).get();
		LOG.info("Result: {}", ar);
	}

	private void configureSystemInfoKb() {
		GraphPattern gp = new GraphPattern(this.pm, """
				?system a ex:System .
				""");

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp,
				"sensorInfoKI");

		this.systemInfoKb.register(answerKI, (_, _) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("system", "<https://www.example.org/Camera>");
			bs.add(b);
			return bs;
		});
	}

	private void configureCausesKb() {
		GraphPattern diagnoseGp2 = new GraphPattern(this.pm, """
				?system ex:isEffectedBy ?component .
				""");
		AnswerKnowledgeInteraction answerKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), diagnoseGp2,
				"sensorInfoKI2");
		this.causesKb.register(answerKI2, (_, _) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("system", "<https://www.example.org/Camera>");
			b.put("component", "<https://www.example.org/Lens>");
			bs.add(b);
			b = new Binding();
			b.put("system", "<https://www.example.org/Camera>");
			b.put("component", "<https://www.example.org/LightSensor>");
			bs.add(b);
			b = new Binding();
			b.put("system", "<https://www.example.org/Camera>");
			b.put("component", "<https://www.example.org/Battery>");
			bs.add(b);
			return bs;
		});
	}

	private void configureWeatherKb() {
		GraphPattern gp = new GraphPattern(this.pm, """
				?weather ex:hasState ?weatherState .
				""");

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp, "weatherKI");
		this.weatherKb.register(answerKI, (_, _) -> {

			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("weather", "<https://www.example.org/weather>");
			b.put("weatherState", "<https://www.example.org/fog>");
			bs.add(b);
			return bs;
		});
	}

	private AskKnowledgeInteraction configureDiagnoseKb() {
		GraphPattern diagnoseGp = new GraphPattern(this.pm, """
				?system a ex:System .
				?system ex:hasAnomaly ?anomaly .
				?anomaly ex:hasCause ?component .
				?component ex:hasProbabilityToBeTheCause ?probability .
				""");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), diagnoseGp, "diagnoseKI");
		this.diagnoseKb.register(askKI);

		String rules = """

				@prefix ex: <https://www.example.org/>

				-> ( <https://www.example.org/Camera> ex:hasAnomaly <https://www.example.org/LowVideoQuality> ) .

				( ?system ex:hasAnomaly ?anomaly ) ( ?system ex:isEffectedBy ?component )
					->
			    ( ?anomaly ex:hasCause ?component ) .
			    
			    ( ?component a ex:Lens ) ( ?weather ex:hasState <https://www.example.org/fog> )
				   ->
				( ?component ex:hasProbabilityToBeTheCause <https://www.example.org/high>) .
			    
				""";

		Set<BaseRule> someRules = JenaRules.convertJenaToKeRules(rules);
		Set<Rule> dkRules = new HashSet<Rule>();

		for (BaseRule br : someRules) {
			LOG.info("Rule: {}", br);
			dkRules.add((Rule) br);
		}

		this.diagnoseKb.setReasonerLevel(3);
		this.diagnoseKb.setDomainKnowledge(dkRules);

		return askKI;
	}

}
