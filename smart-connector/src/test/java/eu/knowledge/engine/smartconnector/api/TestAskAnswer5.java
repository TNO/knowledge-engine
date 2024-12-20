package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.EasyKnowledgeBase;

public class TestAskAnswer5 {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswer5.class);

	private static KnowledgeNetwork kn;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		this.kn = new KnowledgeNetwork();
		EasyKnowledgeBase kb1 = new EasyKnowledgeBase("kb1");
		kb1.setReasonerEnabled(true);
		kn.addKB(kb1);
		EasyKnowledgeBase kb2 = new EasyKnowledgeBase("kb2");
		kb2.setReasonerEnabled(true);
		kn.addKB(kb2);
		EasyKnowledgeBase kb3 = new EasyKnowledgeBase("kb3");
		kb3.setReasonerEnabled(true);
		kn.addKB(kb3);
		EasyKnowledgeBase kb4 = new EasyKnowledgeBase("kb4");
		kb4.setReasonerEnabled(true);
		kn.addKB(kb4);

		GraphPattern gp1 = new GraphPattern(prefixes, """
				?p ex:type ex:Sensor .
				?p ex:hasV ?q .""");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("p", "<https://www.tno.nl/example/sensor1>");
			binding.put("q", "<https://www.tno.nl/example/value1>");
			bindingSet.add(binding);

			binding = new Binding();
			binding.put("p", "<https://www.tno.nl/example/sensor2>");
			binding.put("q", "<https://www.tno.nl/example/value2>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp3 = new GraphPattern(prefixes, """
				?x ex:type ex:Sensor .
				?x ex:inRoom ?y .""");
		AnswerKnowledgeInteraction aKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb3.register(aKI3, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("x", "<https://www.tno.nl/example/sensor1>");
			binding.put("y", "<https://www.tno.nl/example/room1>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp4 = new GraphPattern(prefixes, """
				?f ex:type ex:Sensor .
				?f ex:hasManu ?g""");
		AnswerKnowledgeInteraction aKI4 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp4);
		kb4.register(aKI4, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("f", "<https://www.tno.nl/example/sensor1>");
			binding.put("g", "<https://www.tno.nl/example/manu1>");
			bindingSet.add(binding);
			binding = new Binding();
			binding.put("f", "<https://www.tno.nl/example/sensor3>");
			binding.put("g", "<https://www.tno.nl/example/manu3>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, """
				?a ex:type ex:Sensor .
				?a ex:hasV ?b .
				?a ex:inRoom ?c .
				?a ex:hasManu ?d""");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kb2.register(askKI);
		LOG.info("Waiting until everyone is up-to-date!");
		kn.sync();
		LOG.info("Everyone is up-to-date!");

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			LOG.trace("Before ask.");
			AskPlan aPlan = kb2.planAsk(askKI, new RecipientSelector());

			aPlan.getReasonerPlan().getStore().printGraphVizCode(aPlan.getReasonerPlan());

			result = aPlan.execute(new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");

			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(
					new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId(),
							kb4.getKnowledgeBaseId())),
					kbIds, "The result should come from kb1, kb3, kb4 and not: " + kbIds);

			assertEquals(1, bindings.size());

			for (Binding b : bindings) {
				assertTrue(b.containsKey("a"));
				assertTrue(b.containsKey("b"));
				assertTrue(b.containsKey("c"));
				assertTrue(b.containsKey("d"));
				LOG.info("Binding: {}", b);
			}
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswer5.class.getSimpleName());
		kn.stop();
	}
}
