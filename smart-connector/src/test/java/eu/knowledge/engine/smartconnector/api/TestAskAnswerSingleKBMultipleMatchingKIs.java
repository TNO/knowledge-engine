package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.List;
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

public class TestAskAnswerSingleKBMultipleMatchingKIs {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerSingleKBMultipleMatchingKIs.class);

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

		GraphPattern gp = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI1 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp);
		kb1.register(aKI1, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
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

		gp = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AnswerKnowledgeInteraction aKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp);
		kb1.register(aKI2, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
					"Should not have bindings in this binding set.");

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

		kn.sync();

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

			// with the reasoner there used to be more matching gp (i.e. the meta
			// KIs), but after changing the meta GPs to remove the generic triples, we
			// have the expected size of 2 again.
			assertEquals(2, kbIds.size());

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
