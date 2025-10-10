package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class TestDuplicateBindings {

	private static final Logger LOG = LoggerFactory.getLogger(TestDuplicateBindings.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;
	private static KnowledgeNetwork kn;

	private static AnswerKnowledgeInteraction answerKI1;
	private static AnswerKnowledgeInteraction answerKI3;
	private static AskKnowledgeInteraction askKI2;

	private static ReactKnowledgeInteraction reactKI1;

	private static ReactKnowledgeInteraction reactKI3;

	private static PostKnowledgeInteraction postKI2;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);
		kb3 = new KnowledgeBaseImpl("kb3");
		kn.addKB(kb3);

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		GraphPattern gp1_1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		GraphPattern gp1_2 = new GraphPattern(prefixes, "?b <https://www.tno.nl/example/c> ?d.");
		answerKI1 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1_1);
		kb1.register(answerKI1, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			binding.put("c", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});

		reactKI1 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp1_1, gp1_2);
		kb1.register(reactKI1, (ReactHandler) (aRKI, aReactExchangeInfo) -> {
			assertFalse(aReactExchangeInfo.getArgumentBindings().isEmpty());

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("b", "<https://www.tno.nl/example/b>");
			binding.put("d", "<https://www.tno.nl/example/d>");
			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp3_1 = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		GraphPattern gp3_2 = new GraphPattern(prefixes, "?f <https://www.tno.nl/example/c> ?g.");

		answerKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3_1);
		kb3.register(answerKI3, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("d", "<https://www.tno.nl/example/a>");
			binding.put("e", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});

		reactKI3 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3_1, gp3_2);
		kb3.register(reactKI3, (ReactHandler) (aRKI, aReactExchangeInfo) -> {
			assertFalse(aReactExchangeInfo.getArgumentBindings().isEmpty());

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("f", "<https://www.tno.nl/example/b>");
			binding.put("g", "<https://www.tno.nl/example/d>");
			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp2_1 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		GraphPattern gp2_2 = new GraphPattern(prefixes, "?w <https://www.tno.nl/example/c> ?z.");
		askKI2 = new AskKnowledgeInteraction(new CommunicativeAct(), gp2_1);
		kb2.register(askKI2);
		postKI2 = new PostKnowledgeInteraction(new CommunicativeAct(), gp2_1, gp2_2);
		kb2.register(postKI2);
		kn.sync();
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			LOG.trace("Before ask.");
			result = kb2.ask(askKI2, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");

			LOG.info("Bindings: {}", bindings);
			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId())), kbIds,
					"The result should come from kb1 and kb3 and not: " + kbIds);

			assertEquals(1, bindings.size(),
					"Both KB give the same binding, so they should be merged and result in a single Binding.");

			for (Binding b : bindings) {
				assertTrue(b.containsKey("x"));
				assertTrue(b.containsKey("y"));
				assertEquals(b.get("x"), "<https://www.tno.nl/example/a>");
				assertEquals(b.get("y"), "<https://www.tno.nl/example/c>");
			}
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	@Test
	public void testPostReact() throws InterruptedException {

		// start testing!
		BindingSet bindings = null;
		PostResult result = null;
		try {
			LOG.trace("Before post.");

			var bindingset = new BindingSet();
			var binding = new Binding();
			binding.put("x", "<https://www.tno.nl/example/a>");
			binding.put("y", "<https://www.tno.nl/example/c>");
			bindingset.add(binding);

			result = kb2.post(postKI2, bindingset).get();
			bindings = result.getBindings();
			LOG.trace("After post.");
			LOG.info("Bindings: {}", bindings);

			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(PostExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId())), kbIds,
					"The result should come from kb1 and kb3 and not: " + kbIds);

			assertEquals(1, bindings.size(),
					"Both KB give the same binding, so they should be merged and result in a single Binding.");

			for (Binding b : bindings) {
				assertTrue(b.containsKey("w"));
				assertTrue(b.containsKey("z"));
				assertEquals(b.get("w"), "<https://www.tno.nl/example/b>");
				assertEquals(b.get("z"), "<https://www.tno.nl/example/d>");
			}
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestAskAnswer3.class.getSimpleName());
		kn.stop().get();
	}
}
