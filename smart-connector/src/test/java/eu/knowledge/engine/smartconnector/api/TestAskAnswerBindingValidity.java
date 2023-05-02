package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Status;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class TestAskAnswerBindingValidity {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerBindingValidity.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	@Test
	public void testAskAnswerInvalidOutgoingBindings() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://example.org/");

		var kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kn.addKB(kb2);

		final AtomicBoolean wasInAnswerHandler = new AtomicBoolean(false);
		GraphPattern gp1 = new GraphPattern(prefixes, "?city ex:capitalOf ?country.");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(anAnswerExchangeInfo.getIncomingBindings().iterator().next().get("country")
					.equals("<https://example.org/france>"));
			wasInAnswerHandler.set(true);

			// even though the incoming binding asks for the capital of france, we're
			// answering with the capital of germany anyway (wrong!)

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("city", "<https://example.org/berlin>");
			binding.put("country", "<https://example.org/germany>");
			bindingSet.add(binding);
			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, "?city ex:capitalOf ?country.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kb2.register(askKI);

		kn.sync();

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			// Ask for the capital of france
			var incomingBindings = new BindingSet();
			var b = new Binding();
			b.put("country", "<https://example.org/france>");
			incomingBindings.add(b);
			result = kb2.ask(askKI, incomingBindings).get();
			bindings = result.getBindings();
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		var aei = result.getExchangeInfoPerKnowledgeBase().stream().findAny().get();
		assertEquals(kb1.getKnowledgeBaseId(), aei.getKnowledgeBaseId());
		assertEquals(aei.status, Status.FAILED);
		assertNotNull(aei.failedMessage);
		assertFalse(bindings.iterator().hasNext(), "there should not be any bindings!");
		assertTrue(wasInAnswerHandler.get(), "answer handler should have been called!");
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswerBindingValidity.class.getSimpleName());
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
