package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
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

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class TestAskAnswer {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswer.class);

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

		LOG.info("Waiting for ready...");

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> <https://www.tno.nl/example/c>.");

		CommunicativeAct act1 = new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
				new HashSet<>(Arrays.asList(Vocab.RETRIEVE_KNOWLEDGE_PURPOSE)));
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(act1, gp1);
		kb1.register(aKI, (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			bindingSet.add(binding);

			return bindingSet;
		});
		LOG.info("Registered first AKI");


		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		CommunicativeAct act2 = new CommunicativeAct(new HashSet<>(Arrays.asList(Vocab.INFORM_PURPOSE)),
				new HashSet<>(Arrays.asList(Vocab.RETRIEVE_KNOWLEDGE_PURPOSE)));
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(act2, gp2);
		kb2.register(askKI);
		LOG.info("Registered second AKI");
		kn.sync();
		LOG.info("Synced knowledge network");

		BindingSet bindings = null;
		try {
			Instant start = Instant.now();

			LOG.trace("Before ask");
			AskResult result = kb2.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();

			LOG.info("After ask. It took {}ms ({}ms exchanging)", Duration.between(start, Instant.now()).toMillis(),
					result.getTotalExchangeTime().toMillis());
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertTrue(!b.containsKey("a") && !b.containsKey("c"),
				"The variable names should follow the graph pattern of the requesting KB.");
		
		assertEquals("<https://www.tno.nl/example/a>", b.get("x"), "Binding of 'x' is incorrect.");
		assertEquals("<https://www.tno.nl/example/c>", b.get("y"), "Binding of 'y' is incorrect.");

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswer.class.getSimpleName());
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
