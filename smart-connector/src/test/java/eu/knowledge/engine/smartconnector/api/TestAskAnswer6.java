package eu.knowledge.engine.smartconnector.api;

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

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class TestAskAnswer6 {

	private static final int MAX_SECONDS = 10000;

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswer6.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;
	private static KnowledgeBaseImpl kb4;
//	private static KnowledgeBaseImpl kb5;
//	private static KnowledgeBaseImpl kb6;
//	private static KnowledgeBaseImpl kb7;
//	private static KnowledgeBaseImpl kb8;
//	private static KnowledgeBaseImpl kb9;

	private PrefixMappingMem prefixes;

	private String graphPattern1 = """
			?a <https://www.tno.nl/example/b1> ?c1 .
			?a <https://www.tno.nl/example/b3> ?c3 .
			?a <https://www.tno.nl/example/b2> ?c2 .
			""";

	private String graphPattern2 = """
			?a <https://www.tno.nl/example/b2> ?c2 .
			?a <https://www.tno.nl/example/b3> ?c3 .
			""";

	private String graphPattern3 = """
			?a <https://www.tno.nl/example/b2> ?c2 .
			?a <https://www.tno.nl/example/b1> ?c1 .
			""";

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		var kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);

		kb1.setReasonerLevel(2);

		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);
		kb3 = new KnowledgeBaseImpl("kb3");
		kn.addKB(kb3);
		kb4 = new KnowledgeBaseImpl("kb4");
		kn.addKB(kb4);
//		kb5 = new KnowledgeBaseImpl("kb5");
//		kn.addKB(kb5);
//		kb6 = new KnowledgeBaseImpl("kb6");
//		kn.addKB(kb6);
//		kb7 = new KnowledgeBaseImpl("kb7");
//		kn.addKB(kb7);
//		kb8 = new KnowledgeBaseImpl("kb8");
//		kn.addKB(kb8);
//		kb9 = new KnowledgeBaseImpl("kb9");
//		kn.addKB(kb9);

		GraphPattern gp = new GraphPattern(prefixes, this.graphPattern1);
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
		kb1.register(askKI);

		createKI(kb2, this.graphPattern1);
		createKI(kb3, this.graphPattern3);
		createKI(kb4, this.graphPattern2);
//		createKI(kb5);
//		createKI(kb6);
//		createKI(kb7);
//		createKI(kb8);
//		createKI(kb9);

		kn.sync();

		// start testing!
		BindingSet bindings = null;
		try {
			LOG.info("Before ask.");
			long start = System.currentTimeMillis();
			AskResult result = kb1.ask(askKI, new BindingSet()).get();
			long delta = System.currentTimeMillis() - start;
			result.getReasonerPlan().getStore().printGraphVizCode(result.getReasonerPlan());
			assertTrue(delta < MAX_SECONDS,
					"It should take " + MAX_SECONDS + "ms maximum to finish this test, but was " + delta + "ms.");
			LOG.info("After ask.");
			bindings = result.getBindings();
		} catch (InterruptedException | ExecutionException e) {
			fail(e);
		}

		Iterator<Binding> iter = bindings.iterator();

		assertFalse(iter.hasNext(), "there should be no bindings");
	}

	private void createKI(KnowledgeBaseImpl aKb, String aGraphPattern) {
		GraphPattern gp1 = new GraphPattern(prefixes, aGraphPattern);
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		aKb.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			// this test is about the graph pattern matching, so we can just return an empty
			// binding.
			BindingSet bindingSet = new BindingSet();

			return bindingSet;
		});
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskAnswer6.class.getSimpleName());
		cleanKB(kb1);
		cleanKB(kb2);
		cleanKB(kb3);
		cleanKB(kb4);
//		cleanKB(kb5);
//		cleanKB(kb6);
//		cleanKB(kb7);
//		cleanKB(kb8);
//		cleanKB(kb9);

	}

	private static void cleanKB(KnowledgeBaseImpl aKb) {
		if (aKb != null) {
			aKb.stop();
		} else {
			fail("KB should not be null!");
		}
	}
}
