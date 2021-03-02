package eu.interconnectproject.knowledge_engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

public class TestAskAnswer {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswer.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		int wait = 2;
		final CountDownLatch kb2ReceivedData = new CountDownLatch(1);

		kb1 = new MockedKnowledgeBase("kb1") {
			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				LOG.info("smartConnector of {} ready.", this.name);
			}
		};

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");

		CommunicativeAct act1 = new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE)),
				new HashSet<Resource>(Arrays.asList(Vocab.RETRIEVE_KNOWLEDGE_PURPOSE)));
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(act1, gp1);
		kb1.getSmartConnector().register(aKI, (AnswerHandler) (anAKI, aBindingSet) -> {
			assertTrue(aBindingSet.isEmpty(), "Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			binding.put("c", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});

		Thread.sleep(5000);

		kb2 = new MockedKnowledgeBase("kb2") {
			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				LOG.info("smartConnector of {} ready.", this.name);

			}

		};

		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		CommunicativeAct act2 = new CommunicativeAct(new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE)),
				new HashSet<Resource>(Arrays.asList(Vocab.RETRIEVE_KNOWLEDGE_PURPOSE)));
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(act2, gp2);

		kb2.getSmartConnector().register(askKI);
		LOG.trace("After kb2 register");
		Thread.sleep(10000);

		BindingSet bindings = null;
		try {
			LOG.trace("Before ask.");
			AskResult result = kb2.getSmartConnector().ask(askKI, new BindingSet()).get();
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

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");
		kb2ReceivedData.countDown();

		assertTrue(kb2ReceivedData.await(wait, TimeUnit.SECONDS),
				"KB2 should have initialized within " + wait + " seconds.");

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
