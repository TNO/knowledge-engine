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
import eu.knowledge.engine.smartconnector.impl.Util;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

/**
 * This unit tests tries to execute a decision tree using the reasoning
 * capabilities of the KE. The decision tree is encoded in domain knowledge (in
 * the form of domain rules) that get taken into account when asking or posting
 * data.
 * 
 * Story: A camera sensor publishes observations of the quality of
 * the camera image in terms of contrast and brightness, which is analyzed by the
 * anomaly detector. When the brightness suddenly goes down, the anomaly detector
 * will produce an anomaly, called lowVideoQuality.
 * 
 * A diagnose KB is activated that asks the Knowledge Network which possible
 * causes there are for this anomaly and what the probability for each cause is.
 * 
 * A causes KB can answer for a given system which components of that system
 * are possibly the cause of the anomaly and what type of component it is.
 * This is determined by using a dependency graph that is constructed based 
 * on the physical decomposition of the system. For the camera in our example, 
 * the dependency graph determines that the possible
 * components that can cause are the light sensor, the lens or the battery. 
 * 
 * The probability of a component to be the cause of the anomaly depends on a
 * few possibilities, such as the weather and more detailed information about the
 * component, such as the age. Therefore, a weather KB and a system info KB are 
 * setup to provide extra information to reason over.
 * 
 * The system info KB tells whether a particular system is old or young and
 * the number of charging cycles if it is a battery.
 * 
 * The weather KB tells whether there is currently a sandstorm or fog in the area.
 * It also states at which period of the day this weather state holds .
 * 
 * Then this unit test starts with these possible causes. There are multiple
 * rules for deriving the more detailed cause of the anomaly, such as weather
 * situation, period of the day or usage history of the component.
 * 
 * The decision tree and the rules derived from it are extensive and intended to
 * test more complex situations to reason over.
 * 
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
		b.put("system", "<https://www.example.org/camera>");
		b.put("anomaly", "<https://www.example.org/lowVideoQuality>");
		bindingSet.add(b);

		AskResult ar = this.diagnoseKb.ask(askKI, bindingSet).get();
		LOG.info("Result: {}", ar);
		for (Binding rb : ar.getBindings()) {
			LOG.info("Binding: {}", rb);
		}
	}

	private AskKnowledgeInteraction configureDiagnoseKb() {
		GraphPattern diagnoseGp = new GraphPattern(this.pm, """
				?system rdf:type ex:System .
				?system ex:hasAnomaly ?anomaly .
				?anomaly ex:hasCause ?component .
				?component ex:hasProbabilityToBeTheCause ?probability .
				""");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), diagnoseGp, "diagnoseKI");
		this.diagnoseKb.register(askKI);

		String rules = """
				@prefix ex: <https://www.example.org/>

				-> ( <https://www.example.org/camera> ex:hasAnomaly <https://www.example.org/lowVideoQuality> ) .

				( ?system ex:hasAnomaly ?anomaly ) ( ?system ex:isAffectedBy ?component )
					->
			    ( ?anomaly ex:hasCause ?component ) .
			    

				-> ( <https://www.example.org/interference> rdf:type ex:Interference ) .

				( ?weather rdf:type ex:Weather) ( ?weather ex:hasState <https://www.example.org/fog> ) ( ?interference rdf:type ex:Interference )
					->
			    ( ?interference ex:hasLevel <https://www.example.org/high> ) .

			    ( ?component rdf:type ex:Lens ) ( ?interference ex:hasLevel <https://www.example.org/high> )
				   ->
				( ?component ex:hasProbabilityToBeTheCause <https://www.example.org/high>) .
				
				
				-> ( <https://www.example.org/lightintensity> rdf:type ex:LightIntensity ) .

				( ?weather rdf:type ex:Weather) ( ?weather ex:atPeriodOfDay <https://www.example.org/sunset> ) ( ?lightIntensity rdf:type ex:LightIntensity )
					->
			    ( ?lightIntensity ex:hasLevel <https://www.example.org/low> ) .

			    ( ?component rdf:type ex:Lens ) ( ?lightIntensity ex:hasLevel <https://www.example.org/low> )
				   ->
				( ?component ex:hasProbabilityToBeTheCause <https://www.example.org/medium>) .


				( ?component rdf:type ex:Battery ) ( ?component ex:hasNrOfCycles <https://www.example.org/high> )
					->
				( ?component ex:hasMaximumCapacity <https://www.example.org/low> ) .

				( ?component rdf:type ex:Battery ) ( ?component ex:hasAge <https://www.example.org/old> )
					->
				( ?component ex:hasMaximumCapacity <https://www.example.org/low> ) .

			    ( ?component rdf:type ex:Battery ) ( ?component ex:hasMaximumCapacity <https://www.example.org/low> )
				   ->
				( ?component ex:hasProbabilityToBeTheCause <https://www.example.org/low>) .				
				
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

	private void configureCausesKb() {
		GraphPattern diagnoseGp2 = new GraphPattern(this.pm, """
				?system ex:isAffectedBy ?component .
				?component rdf:type ?type .
				""");
		AnswerKnowledgeInteraction answerKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), diagnoseGp2,
				"causesKI");
		this.causesKb.register(answerKI2, (_, ei) -> {
			LOG.info("{}",ei.getIncomingBindings());
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("system", "<https://www.example.org/camera>");
			b.put("component", "<https://www.example.org/lens>");
			b.put("type", "<https://www.example.org/Lens>");
			bs.add(b);
			b = new Binding();
			b.put("system", "<https://www.example.org/camera>");
			b.put("component", "<https://www.example.org/lightSensor>");
			b.put("type", "<https://www.example.org/LightSensor>");
			bs.add(b);
			b = new Binding();
			b.put("system", "<https://www.example.org/camera>");
			b.put("component", "<https://www.example.org/battery>");
			b.put("type", "<https://www.example.org/Battery>");
			bs.add(b);
			var bs1 = filterOutgoingBindingSet(ei.getIncomingBindings(),bs);
			return bs1;
		});
	}

	private void configureWeatherKb() {
		GraphPattern gp = new GraphPattern(this.pm, """
				?weather a ex:Weather .
				?weather ex:hasState ?weatherState .
				?weather ex:atPeriodOfDay ?periodOfDay .
				""");

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp, "weatherKI");
		this.weatherKb.register(answerKI, (_, ei) -> {

			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("weather", "<https://www.example.org/weather>");
			b.put("weatherState", "<https://www.example.org/fog>");
			b.put("periodOfDay", "<https://www.example.org/sunset>");
			bs.add(b);
			var bs1 = filterOutgoingBindingSet(ei.getIncomingBindings(),bs);
			return bs1;
		});
	}

	private void configureSystemInfoKb() {
		GraphPattern gp = new GraphPattern(this.pm, """
				?system rdf:type ex:System .
				?system ex:hasAge ?age .
				?system ex:hasNrOfCycles ?cycles .
				""");

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp,
				"systemInfoKI");

		this.systemInfoKb.register(answerKI, (_, ei) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("system", "<https://www.example.org/camera>");
			b.put("age", "<https://www.example.org/young>");
			b.put("cycles", "<https://www.example.org/low>");
			bs.add(b);
			b= new Binding();
			b.put("system", "<https://www.example.org/battery>");
			b.put("age", "<https://www.example.org/young>");
			b.put("cycles", "<https://www.example.org/high>");
			bs.add(b);
			var bs1 = filterOutgoingBindingSet(ei.getIncomingBindings(),bs);
			return bs1;
		});
	}

	private BindingSet filterOutgoingBindingSet(BindingSet ib, BindingSet ob) {
		var bs2 = Util.translateFromApiBindingSet(ob);
		Util.removeRedundantBindingsAnswer(Util.translateFromApiBindingSet(ib), bs2);
		
		return Util.translateToApiBindingSet(bs2);
		
	}
	
}
