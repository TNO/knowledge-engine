package eu.knowledge.engine.smartconnector.api;

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
import java.util.stream.Collectors;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@Tag("Long")
public class TestAskAnswerManyKIs {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerManyKIs.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;
	private static MockedKnowledgeBase kb3;
	private static MockedKnowledgeBase kb4;

	private String pattern = "?{} <https://www.tno.nl/example/b{}> ?{}.";

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
		kb4 = new MockedKnowledgeBase("kb4");
		kn.addKB(kb4);

		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();
		LOG.info("Everyone is ready!");

		int count = 50;

		AnswerKnowledgeInteraction[] aKIarray = new AnswerKnowledgeInteraction[count];
		AnswerKnowledgeInteraction[] aKI3array = new AnswerKnowledgeInteraction[count];
		AnswerKnowledgeInteraction[] aKI4array = new AnswerKnowledgeInteraction[count];
		AskKnowledgeInteraction[] askKIarray = new AskKnowledgeInteraction[count];

		for (int i = 0; i < count; i++) {
			final int idx = i;
			GraphPattern gp1 = new GraphPattern(prefixes, this.format("a" + i, "b" + i, "c" + i));
			aKIarray[i] = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
			kb1.register(aKIarray[i], (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
				assertTrue(
						anAnswerExchangeInfo.getIncomingBindings().isEmpty()
								|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
						"Should not have bindings in this binding set.");

				BindingSet bindingSet = new BindingSet();
				Binding binding = new Binding();
				binding.put("a" + idx, "<https://www.tno.nl/example/a" + idx + ">");
				binding.put("c" + idx, "<https://www.tno.nl/example/c" + idx + ">");
				bindingSet.add(binding);

				return bindingSet;
			});

			GraphPattern gp3 = new GraphPattern(prefixes, this.format("d" + i, "b" + i, "e" + i));
			aKI3array[i] = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
			kb3.register(aKI3array[i], (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
				assertTrue(
						anAnswerExchangeInfo.getIncomingBindings().isEmpty()
								|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
						"Should not have bindings in this binding set.");

				BindingSet bindingSet = new BindingSet();
				Binding binding = new Binding();
				binding.put("d" + idx, "<https://www.tno.nl/example/d" + idx + ">");
				binding.put("e" + idx, "<https://www.tno.nl/example/e" + idx + ">");
				bindingSet.add(binding);

				return bindingSet;
			});

			GraphPattern gp4 = new GraphPattern(prefixes, this.format("f" + i, "b" + i, "g" + i));
			aKI4array[i] = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp4);
			kb4.register(aKI4array[i], (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
				assertTrue(
						anAnswerExchangeInfo.getIncomingBindings().isEmpty()
								|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().size() == 0,
						"Should not have bindings in this binding set.");

				BindingSet bindingSet = new BindingSet();
				Binding binding = new Binding();
				binding.put("f" + idx, "<https://www.tno.nl/example/f" + idx + ">");
				binding.put("g" + idx, "<https://www.tno.nl/example/g" + idx + ">");
				bindingSet.add(binding);

				return bindingSet;
			});

			GraphPattern gp2 = new GraphPattern(prefixes, this.format("x" + i, "b" + i, "y" + i));
			askKIarray[i] = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
			kb2.register(askKIarray[i]);
		}

		LOG.info("Waiting for upToDate...");
		kn.waitForUpToDate();

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			int idx = count / 2;

			LOG.trace("Before ask.");
			result = kb2.ask(askKIarray[idx], new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");
			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(ExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(
					new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId(),
							kb4.getKnowledgeBaseId())),
					kbIds, "The result should come from kb1, kb3, kb4 and not: " + kbIds);

			assertEquals(3, bindings.size());

			for (Binding b : bindings) {
				assertTrue(b.containsKey("x" + idx));
				assertTrue(b.containsKey("y" + idx));
				LOG.info("Binding: {}", b);
			}
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	private String format(Object... objects) {

		String chars = MessageFormatter.arrayFormat(pattern, objects).getMessage();
		return chars;
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswerManyKIs.class.getSimpleName());
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

		if (kb3 != null) {
			kb3.stop();
		} else {
			fail("KB3 should not be null!");
		}

		if (kb4 != null) {
			kb4.stop();
		} else {
			fail("KB4 should not be null!");
		}
	}
}
