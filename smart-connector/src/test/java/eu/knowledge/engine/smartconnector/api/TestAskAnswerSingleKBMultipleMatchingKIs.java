package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;

public class TestAskAnswerSingleKBMultipleMatchingKIs {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerSingleKBMultipleMatchingKIs.class);

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

		var kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kn.addKB(kb2);

		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		GraphPattern gp = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI1 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp);
		kb1.register(aKI1, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(anAnswerExchangeInfo.getIncomingBindings().isEmpty() || anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0, "Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			binding.put("c", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});

		gp = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AnswerKnowledgeInteraction aKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp);
		kb1.register(aKI2, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(anAnswerExchangeInfo.getIncomingBindings().isEmpty() || anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0, "Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("x", "<https://www.tno.nl/example/x>");
			binding.put("y", "<https://www.tno.nl/example/y>");
			bindingSet.add(binding);

			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, "?p <https://www.tno.nl/example/b> ?q.");
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

			List<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toList());

			// with the reasoner there are more matching gp (i.e. the meta KIs)
			assertEquals(3, kbIds.size());

			for (Binding b : bindings) {
				assertTrue(b.containsKey("p"));
				assertTrue(b.containsKey("q"));
				LOG.info("Binding: {}", b);
			}
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswerSingleKBMultipleMatchingKIs.class.getSimpleName());
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
