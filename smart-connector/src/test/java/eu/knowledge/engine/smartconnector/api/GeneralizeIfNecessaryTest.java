package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

/**
 * This test tries to illustrate <i>forward compatibility</i>. We instantiate an
 * App KB and a Lamp1 KB that exchange data about the on/off state of the lamp.
 * When a new Lamp2 KB is introduced that does not support on/off because it is
 * dimmable, we should how the reasoner+domain knowledge combination is able to
 * make the app work with the new dimmable type of lamp although it was not
 * designed to work with those types.
 * 
 * @author nouwtb
 *
 */
public class GeneralizeIfNecessaryTest {

	private static final Logger LOG = LoggerFactory.getLogger(GeneralizeIfNecessaryTest.class);

	private static KnowledgeNetwork kn = new KnowledgeNetwork();
	private MockedKnowledgeBase appKb = new MockedKnowledgeBase("AppKB");
	private MockedKnowledgeBase sensor1Kb = new MockedKnowledgeBase("Sensor1KB");
	private MockedKnowledgeBase sensor2Kb = new MockedKnowledgeBase("Sensor2KB");
	private MockedKnowledgeBase floorplanKb = new MockedKnowledgeBase("FloorplanKB");
	private PrefixMapping prefixes = new PrefixMappingMem().setNsPrefix("ex", "http://example.org/").setNsPrefix("rdf",
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#");

	@Test
	public void test() throws InterruptedException, ExecutionException {

		kn.addKB(appKb);
		this.appKb.setReasonerEnabled(true);
		kn.addKB(sensor1Kb);
		kn.addKB(sensor2Kb);
		kn.addKB(floorplanKb);

		AskKnowledgeInteraction appKbAsk = configureAppKb();
		configureSensor1Kb();
		configureSensor2Kb();
		configureFloorplanKb();

		kn.sync();

		BindingSet argument = new BindingSet();
		Binding binding = new Binding();
		argument.add(binding);
		AskPlan pp = appKb.planAsk(appKbAsk, new RecipientSelector());

		pp.getReasonerPlan().getStore().printGraphVizCode(pp.getReasonerPlan());
		AskResult result = pp.execute(argument).get();

		LOG.info("Result bindings: {}", result.getBindings());

	}

	private AskKnowledgeInteraction configureAppKb() {
		GraphPattern appGP = new GraphPattern(prefixes,
				"?s rdf:type ex:Sensor . ?s ex:isPartOf ex:building1 . ?s ex:hasLatestValue ?value .");
		AskKnowledgeInteraction appKbAsk = new AskKnowledgeInteraction(new CommunicativeAct(), appGP);
		appKb.register(appKbAsk);

		HashSet<TriplePattern> rule1ant = new HashSet<>(
				Arrays.asList(new TriplePattern("?x <http://example.org/isPartOf> ?y"),
						new TriplePattern("?y <http://example.org/isPartOf> ?z")));
		HashSet<TriplePattern> rule1con = new HashSet<>(
				Arrays.asList(new TriplePattern("?x <http://example.org/isPartOf> ?z")));

		Rule rule1 = new Rule(rule1ant, rule1con);
		appKb.setDomainKnowledge(new HashSet<>(Arrays.asList(rule1)));

		return appKbAsk;
	}

	private void configureSensor1Kb() {
		GraphPattern sensor1GP = new GraphPattern(prefixes,
				"ex:sensor1 rdf:type ex:Sensor . ex:sensor1 ex:isPartOf ex:room1 . ex:sensor1 ex:hasLatestValue ?value .");

		AnswerKnowledgeInteraction sensor1KbAnswer = new AnswerKnowledgeInteraction(new CommunicativeAct(), sensor1GP);
		sensor1Kb.register(sensor1KbAnswer, (AnswerHandler) (aKI, aAnswerInfo) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("value", "10");
			bs.add(b);
			return bs;
		});
	}

	private void configureSensor2Kb() {
		GraphPattern sensor1GP = new GraphPattern(prefixes,
				"ex:sensor2 rdf:type ex:Sensor . ex:sensor2 ex:isPartOf ex:room2 . ex:sensor2 ex:hasLatestValue ?value .");

		AnswerKnowledgeInteraction sensor1KbAnswer = new AnswerKnowledgeInteraction(new CommunicativeAct(), sensor1GP);
		sensor2Kb.register(sensor1KbAnswer, (AnswerHandler) (aKI, aAnswerInfo) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("value", "20");
			bs.add(b);
			return bs;
		});
	}

	private void configureFloorplanKb() {
		GraphPattern floorplanGP = new GraphPattern(prefixes, "?x ex:isPartOf ?y .");

		AnswerKnowledgeInteraction floorplanKbAnswer = new AnswerKnowledgeInteraction(new CommunicativeAct(),
				floorplanGP);
		floorplanKb.register(floorplanKbAnswer, (AnswerHandler) (aKI, aAnswerInfo) -> {
			BindingSet bs = new BindingSet();

			Binding b = new Binding();
			b.put("x", "<http://example.org/floor1>");
			b.put("y", "<http://example.org/building1>");
			bs.add(b);

			b = new Binding();
			b.put("x", "<http://example.org/room1>");
			b.put("y", "<http://example.org/floor1>");
			bs.add(b);

			b = new Binding();
			b.put("x", "<http://example.org/room2>");
			b.put("y", "<http://example.org/floor1>");
			bs.add(b);

			return bs;
		});
	}

	@AfterAll
	public static void close() {
		LOG.info("Clean up: {}", GeneralizeIfNecessaryTest.class.getSimpleName());
		try {
			kn.stop().get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Stopping the Knowledge Network should succeed: {}", e);
		}
	}

}
