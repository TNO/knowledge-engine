package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

public class TestAskRecipientSelector {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskRecipientSelector.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;
	private static KnowledgeBaseImpl kb4;

	private static KnowledgeNetwork kn;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);
		kb3 = new KnowledgeBaseImpl("kb3");
		kn.addKB(kb3);
		kb4 = new KnowledgeBaseImpl("kb4");
		kn.addKB(kb4);

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			binding.put("c", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp3 = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		AnswerKnowledgeInteraction aKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb3.register(aKI3, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("d", "<https://www.tno.nl/example/d>");
			binding.put("e", "<https://www.tno.nl/example/e>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp4 = new GraphPattern(prefixes, "?f <https://www.tno.nl/example/b> ?g.");
		AnswerKnowledgeInteraction aKI4 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp4);
		kb4.register(aKI4, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("f", "<https://www.tno.nl/example/f>");
			binding.put("g", "<https://www.tno.nl/example/g>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kb2.register(askKI);

		kn.sync();

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			LOG.trace("Before ask.");

			// RecipientSelector cannot be null.
			assertThrows(IllegalArgumentException.class, () -> {
				kb2.ask(askKI, null, new BindingSet()).get();
			});

			// Recipient Selector is single KB (kb1).
			result = kb2.ask(askKI, new RecipientSelector(kb1.getKnowledgeBaseId()), new BindingSet()).get();
			bindings = result.getBindings();
			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId())), kbIds,
					"The result should come from kb1 only and not: " + kbIds);

			assertEquals(1, bindings.size());

			Binding bind = bindings.iterator().next();
			assertTrue(bind.containsKey("x"));
			assertTrue(bind.containsKey("y"));

			assertEquals(bind.get("x"), "<https://www.tno.nl/example/a>");
			assertEquals(bind.get("y"), "<https://www.tno.nl/example/c>");
			LOG.info("Binding: {}", bind);

			// Recipient Selector is multiple KBs (kb1 & kb3).
			result = kb2.ask(askKI,
					new RecipientSelector(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId())),
					new BindingSet()).get();
			bindings = result.getBindings();
			kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId())), kbIds,
					"The result should come from kb1 and kb3 only and not: " + kbIds);

			assertEquals(2, bindings.size());

			assertTrue(bindings.stream()
					.allMatch(b -> (b.get("x").equals("<https://www.tno.nl/example/a>")
							|| b.get("x").equals("<https://www.tno.nl/example/d>"))
							&& (b.get("y").equals("<https://www.tno.nl/example/c>")
									|| b.get("y").equals("<https://www.tno.nl/example/e>"))));
			assertTrue(bindings.stream().anyMatch(b -> b.get("x").equals("<https://www.tno.nl/example/a>")
					&& b.get("y").equals("<https://www.tno.nl/example/c>")));
			assertTrue(bindings.stream().anyMatch(b -> b.get("x").equals("<https://www.tno.nl/example/d>")
					&& b.get("y").equals("<https://www.tno.nl/example/e>")));

			// Recipient Selector asks all Knowledge Bases (kb1, kb3, kb4).

			result = kb2.ask(askKI, new RecipientSelector(), new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");

			kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(
					new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId(),
							kb4.getKnowledgeBaseId())),
					kbIds, "The result should come from kb1, kb3, kb4 and not: " + kbIds);

			assertEquals(3, bindings.size());

			for (Binding b : bindings) {
				assertTrue(b.containsKey("x"));
				assertTrue(b.containsKey("y"));
				LOG.info("Binding: {}", b);
			}
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestAskRecipientSelector.class.getSimpleName());
		kn.stop().get();
	}
}
