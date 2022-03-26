package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What is faster:
 * <ul>
 * <li>To answer with a single large graph pattern
 * <li>To answer with two smaller graph patterns
 * </ul>
 * 
 * @author nouwtb
 *
 */
public class SingleLargeVsMultipleSmallGraphPatternTest {

	private static final Logger LOG = LoggerFactory.getLogger(SingleLargeVsMultipleSmallGraphPatternTest.class);

	private KnowledgeNetwork kn1 = new KnowledgeNetwork();
	private KnowledgeNetwork kn2 = new KnowledgeNetwork();
	private PrefixMapping prefixes = new PrefixMappingMem().setNsPrefix("ex", "https://www.tno.nl/example/");

	private static int NR_OF_BINDINGS = 20;

	@Test
	public void testSingleLargeGP() throws InterruptedException, ExecutionException {

		// creating SCs
		MockedKnowledgeBase kb1 = new MockedKnowledgeBase("KB1");
		kn1.addKB(kb1);
		kb1.setReasonerEnabled(true);
		MockedKnowledgeBase kb2 = new MockedKnowledgeBase("KB2");
		kn1.addKB(kb2);
		kb2.setReasonerEnabled(true);

		kn1.startAndWaitForReady();

		// prepare large binding
		final BindingSet bs = new BindingSet();
		Binding b1;
		for (int i = 0; i < NR_OF_BINDINGS; i = i + 10) {
			b1 = new Binding();

			b1.put("s", "<s" + i + ">");
			b1.put("o1", "<o" + (i + 1) + ">");
			b1.put("o2", "<o" + (i + 2) + ">");
			b1.put("o3", "<o" + (i + 3) + ">");
			b1.put("o4", "<o" + (i + 4) + ">");
			b1.put("o5", "<o" + (i + 5) + ">");
			b1.put("o6", "<o" + (i + 6) + ">");
			b1.put("o7", "<o" + (i + 7) + ">");
			b1.put("o8", "<o" + (i + 8) + ">");
			b1.put("o9", "<o" + (i + 9) + ">");
			b1.put("o10", "<o" + (i + 10) + ">");
			bs.add(b1);
		}

		// adding KIs

		GraphPattern gp1 = new GraphPattern(prefixes, "?s ex:pred1 ?o1.", "?s ex:pred2 ?o2.", "?s ex:pred3 ?o3.",
				"?s ex:pred4 ?o4.", "?s ex:pred5 ?o5.", "?s ex:pred6 ?o6.", "?s ex:pred7 ?o7.", "?s ex:pred8 ?o8.",
				"?s ex:pred9 ?o9.", "?s ex:pred10 ?o10.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(askKI);

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb2.register(answerKI, (AnswerHandler) (bindingSet, answerEI) -> {
			return bs;
		});

		kn1.waitForUpToDate();

		long start = System.nanoTime();
		AskResult ar = kb1.ask(askKI, new BindingSet()).get();
		long end = System.nanoTime();
		LOG.info("Duration: {}s", (((double) end - (double) start) / 1000000000));

		LOG.info("Bindings: {}", ar.getBindings());

		kn1.stop().get();

	}

	@Test
	public void testMultipleSmallGPs() throws InterruptedException, ExecutionException {

		// creating SCs
		MockedKnowledgeBase kb1 = new MockedKnowledgeBase("KB1");
		kn2.addKB(kb1);
		kb1.setReasonerEnabled(true);
		MockedKnowledgeBase kb2 = new MockedKnowledgeBase("KB2");
		kn2.addKB(kb2);
		kb2.setReasonerEnabled(true);

		kn2.startAndWaitForReady();

		// prepare large binding
		final BindingSet bs1 = new BindingSet();
		final BindingSet bs2 = new BindingSet();
		Binding b1, b2;
		for (int i = 0; i < NR_OF_BINDINGS; i = i + 10) {
			b1 = new Binding();
			b2 = new Binding();

			b1.put("s", "<s" + i + ">");
			b1.put("p1", "<p" + (i + 1) + ">");
			b1.put("p2", "<p" + (i + 2) + ">");
			b1.put("p3", "<p" + (i + 3) + ">");
			b1.put("p4", "<p" + (i + 4) + ">");
			b1.put("p5", "<p" + (i + 5) + ">");
			bs1.add(b1);

			b2.put("s", "<s" + i + ">");
			b2.put("p6", "<p" + (i + 6) + ">");
			b2.put("p7", "<p" + (i + 7) + ">");
			b2.put("p8", "<p" + (i + 8) + ">");
			b2.put("p9", "<p" + (i + 9) + ">");
			b2.put("p10", "<p" + (i + 10) + ">");
			bs2.add(b2);
		}

		// adding KIs

		GraphPattern gp1 = new GraphPattern(prefixes, "?s ex:pred1 ?o1.", "?s ex:pred2 ?o2.", "?s ex:pred3 ?o3.",
				"?s ex:pred4 ?o4.", "?s ex:pred5 ?o5.", "?s ex:pred6 ?o6.", "?s ex:pred7 ?o7.", "?s ex:pred8 ?o8.",
				"?s ex:pred9 ?o9.", "?s ex:pred10 ?o10.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(askKI);

		GraphPattern gp2 = new GraphPattern(prefixes, "?s ex:pred1 ?p1.", "?s ex:pred2 ?p2.", "?s ex:pred3 ?p3.",
				"?s ex:pred4 ?p4.", "?s ex:pred5 ?p5.");
		GraphPattern gp3 = new GraphPattern(prefixes, "?s ex:pred6 ?p6.", "?s ex:pred7 ?p7.", "?s ex:pred8 ?p8.",
				"?s ex:pred9 ?p9.", "?s ex:pred10 ?p10.");
		AnswerKnowledgeInteraction answerKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp2);
		AnswerKnowledgeInteraction answerKI3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb2.register(answerKI2, (AnswerHandler) (bindingSet, answerEI) -> {
			return bs1;
		});

		kb2.register(answerKI3, (AnswerHandler) (bindingSet, answerEI) -> {
			return bs2;
		});

		kn2.waitForUpToDate();

		long start = System.nanoTime();
		AskResult ar = kb1.ask(askKI, new BindingSet()).get();
		long end = System.nanoTime();
		LOG.info("Duration: {}s", (((double) end - (double) start) / 1000000000));

		LOG.info("Bindings: {}", ar.getBindings());

		kn2.stop().get();

	}

}
