package eu.interconnectproject.knowledge_engine.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerHandler;
import eu.interconnectproject.knowledge_engine.smartconnector.api.CommunicativeAct;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;

public class TestAskAnswer {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswer.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static AdminUI admin;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
		admin = new AdminUI();
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
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
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
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);

		kb2.getSmartConnector().register(askKI);
		LOG.trace("After kb2 register");
		Thread.sleep(10000);

		BindingSet result = null;
		try {
			LOG.trace("Before ask.");
			result = kb2.getSmartConnector().ask(askKI, new BindingSet()).get().getBindings();
			LOG.trace("After ask.");
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = result.iterator();

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertEquals("<https://www.tno.nl/example/a>", b.get("a"), "Binding of 'a' is incorrect.");
		assertEquals("<https://www.tno.nl/example/c>", b.get("c"), "Binding of 'c' is incorrect.");

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

		admin.close();
	}
}
