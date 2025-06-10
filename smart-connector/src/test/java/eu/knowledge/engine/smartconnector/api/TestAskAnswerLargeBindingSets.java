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
import java.util.concurrent.TimeUnit;
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

public class TestAskAnswerLargeBindingSets {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerLargeBindingSets.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;

	private static BindingSet kb1BS;
	private static BindingSet kb2BS;

	private static char[] chars = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' };

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {

		kb1BS = new BindingSet();

		Binding b;
		for (int i = 0; i < 350; i++) {
			b = new Binding();
			for (char c : chars)
				b.put(Character.toString(c), "<https://www.tno.nl/example/" + c + i + ">");
			kb1BS.add(b);
		}

		kb2BS = new BindingSet();

		for (int i = 350; i < 700; i++) {
			b = new Binding();
			for (char c : chars)
				b.put(Character.toString(c), "<https://www.tno.nl/example/" + c + i + ">");
			kb2BS.add(b);
		}

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
		kb3 = new KnowledgeBaseImpl("kb3");
		kn.addKB(kb3);

		GraphPattern gp1 = new GraphPattern(prefixes, """
				?a <https://www.tno.nl/example/a> ?b .
				?a <https://www.tno.nl/example/b> ?c .
				?a <https://www.tno.nl/example/c> ?d .
				?a <https://www.tno.nl/example/d> ?e .
				?e <https://www.tno.nl/example/e> ?f .
				?e <https://www.tno.nl/example/f> ?g .
				""");

		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			return kb1BS;
		});

		AnswerKnowledgeInteraction aKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb3.register(aKI3, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			return kb2BS;
		});

		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb2.register(askKI);
		LOG.info("Waiting until everyone is up-to-date!");
		kn.sync();
		LOG.info("Everyone is up-to-date!");

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			LOG.trace("Before ask.");

			try {
				result = kb2.ask(askKI, new BindingSet()).get(10, TimeUnit.SECONDS);
			} catch (TimeoutException te) {
				fail("This ASK should take 10 seconds maximally.");
			}

			bindings = result.getBindings();
			LOG.trace("After ask.");

			Set<URI> kbIds = result.getExchangeInfoPerKnowledgeBase().stream().map(AskExchangeInfo::getKnowledgeBaseId)
					.collect(Collectors.toSet());

			assertEquals(new HashSet<URI>(Arrays.asList(kb1.getKnowledgeBaseId(), kb3.getKnowledgeBaseId())), kbIds,
					"The result should come from kb1, kb3 and not: " + kbIds);

			assertEquals(700, bindings.size());

		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswerLargeBindingSets.class.getSimpleName());
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
	}
}
