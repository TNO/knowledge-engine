package eu.interconnectproject.knowledge_engine.smartconnector.api;

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

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAskAnswer3 {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswer3.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;
	private static MockedKnowledgeBase kb3;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		var kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kn.addKB(kb2);
		kb3 = new MockedKnowledgeBase("kb3");
		kn.addKB(kb3);

		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, aBindingSet) -> {
			assertTrue(aBindingSet.isEmpty(), "Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			binding.put("c", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp3 = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		AnswerKnowledgeInteraction aKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb3.register(aKI3, (AnswerHandler) (anAKI, aBindingSet) -> {
			assertTrue(aBindingSet.isEmpty(), "Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("d", "<https://www.tno.nl/example/d>");
			binding.put("e", "<https://www.tno.nl/example/e>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kb2.register(askKI);

		kn.waitForUpToDate();

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			LOG.trace("Before ask.");
			result = kb2.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");
			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().keySet();

			assertEquals(new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId())), kbIds,
					"The result should come from kb1 and kb3 and not: " + kbIds);

			assertEquals(bindings.size(), 2);

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
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswer3.class.getSimpleName());
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
