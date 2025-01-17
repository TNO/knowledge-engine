package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestAskAnswerUnregister {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerUnregister.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		var kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

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

		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kb2.register(askKI);

		kn.sync();

		// start testing!
		BindingSet bindings = null;
		try {
			LOG.trace("Before ask.");
			AskResult result = kb2.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertTrue(!b.containsKey("a") && !b.containsKey("c"),
				"The variable names should follow the graph pattern of the current KB.");

		assertEquals("<https://www.tno.nl/example/a>", b.get("x"), "Binding of 'x' is incorrect.");
		assertEquals("<https://www.tno.nl/example/c>", b.get("y"), "Binding of 'y' is incorrect.");

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding");

		// now unregister everything and register new stuff.
		kb1.unregister(aKI);
		kb2.unregister(askKI);

		GraphPattern gp3 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/p> ?c.");
		AnswerKnowledgeInteraction aKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb1.register(aKI3, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
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

		GraphPattern gp4 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/p> ?y.");
		AskKnowledgeInteraction askKI4 = new AskKnowledgeInteraction(new CommunicativeAct(), gp4);
		kb2.register(askKI4);

		kn.sync();

		// test whether the old knowledge interactions are indeed gone.
		BindingSet bindings2 = null;
		boolean success = false;
		try {
			LOG.info("What happens if we ask a non-existing knwoledge interaction?");
			AskResult result = kb2.ask(askKI, new BindingSet()).get();
			success = true;
		} catch (InterruptedException | ExecutionException | AssertionError e) {
			success = false;
		}
		assertFalse(success, "The ask should fail with a non existing knowledge interaction.");

		// test the new knowledge interactions.
		// start testing!
		BindingSet bindings3 = null;
		try {
			AskResult result = kb2.ask(askKI4, new BindingSet()).get();
			bindings3 = result.getBindings();
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter3 = bindings3.iterator();

		assertTrue(iter3.hasNext(), "there should be at least 1 binding");
		Binding b3 = iter3.next();

		assertTrue(!b3.containsKey("a") && !b3.containsKey("c"),
				"The variable names should follow the graph pattern of the current KB.");

		assertEquals("<https://www.tno.nl/example/a>", b3.get("x"), "Binding of 'x' is incorrect.");
		assertEquals("<https://www.tno.nl/example/c>", b3.get("y"), "Binding of 'y' is incorrect.");

		assertFalse(iter3.hasNext(), "This BindingSet should only have a single binding");

	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswerUnregister.class.getSimpleName());
		if (kb1 != null) {
			kb1.stop();
		} else {
			fail("KB1 should not be null!");
		}

		if (kb2 != null) {

			kb2.stop();
		} else {
			fail("KB2 should not be null!");
		}
	}
}
