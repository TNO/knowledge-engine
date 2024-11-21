package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class TestAskAnswerWithGapsEnabled2 {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerWithGapsEnabled2.class);

	private static MockedKnowledgeBase kbRelationAsker;
	private static MockedKnowledgeBase kbRelationProvider;

	private static KnowledgeNetwork kn;

	private static PrefixMappingMem prefixes;

	private AskKnowledgeInteraction askKIGaps;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {

		prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

	}

	@Test
	public void testAskAnswerWithGapsEnabled() throws InterruptedException, URISyntaxException {

		// In this test there will be an Ask KB with a single AskKI and
		// an AnswerKB with a single AnswerKI that answers only part of the Ask pattern.
		// The test will execute the AskKI with knowledge gaps enabled.
		// As a result, the set of knowledge gaps should contain a single gap.

		setupNetwork();

		// Perform the ASK
		try {
			AskResult result = kbRelationAsker.ask(askKIGaps, new BindingSet()).get();
			// check whether set of knowledge gaps contains a single gap
			Set<KnowledgeGap> gaps = result.getKnowledgeGaps();

			result.getReasonerPlan().getStore().printGraphVizCode(result.getReasonerPlan(), true);

			LOG.info("Found gaps: " + gaps);
			assertFalse(gaps.isEmpty(), "The set of knowledge gaps should be empty");
			assertEquals(1, gaps.size(), "Number of gaps should be 1");
			Iterator<KnowledgeGap> iter = gaps.iterator();
			while (iter.hasNext()) {
				KnowledgeGap gap = iter.next();
				Iterator<TriplePattern> gapiter = gap.iterator();
				while (gapiter.hasNext()) {
					TriplePattern triple = gapiter.next();
					String tpString = FmtUtils.stringForTriple(triple.asTriple(), new PrefixMappingZero());
					LOG.info("Gap is " + tpString);
					assertEquals("?a <https://www.tno.nl/example/isFatherOf> ?c", tpString,
							"Gap should be ?a <https://www.tno.nl/example/isFatherOf> ?c");
				}
			}
			BindingSet bindings = result.getBindings();
			LOG.info("Resulting binding set is: " + bindings);
			assertTrue(bindings.isEmpty(), "The resulting bindingset should be empty");
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

	}

	private void setupNetwork() {

		instantiateAskRelationsKB();
		instantiateAnswerRelationsKB();
		kn = new KnowledgeNetwork();
		kn.addKB(kbRelationAsker);
		kn.addKB(kbRelationProvider);

		long start = System.nanoTime();
		kn.sync();
		long end = System.nanoTime();
		LOG.info("Duration: {}", (((double) (end - start)) / 1_000_000));
	}

	public void instantiateAskRelationsKB() {
		// start a knowledge base with the behavior "I am interested in related people"
		kbRelationAsker = new MockedKnowledgeBase("RelationAsker");
		kbRelationAsker.setReasonerEnabled(true);

		// Register an Ask pattern for relations with knowledge gaps enabled
		GraphPattern gp1 = new GraphPattern(prefixes, "?a ex:isRelatedTo ?b . ?a ex:isFatherOf ?c .");
		this.askKIGaps = new AskKnowledgeInteraction(new CommunicativeAct(), gp1, "askRelations", false, true, true,
				MatchStrategy.SUPREME_LEVEL);
		kbRelationAsker.register(this.askKIGaps);

	}

	public void instantiateAnswerRelationsKB() {
		// start a knowledge base with the behavior "I can supply related people"
		kbRelationProvider = new MockedKnowledgeBase("RelationProvider");
		kbRelationProvider.setReasonerEnabled(true);

		// Patterns for the RelationProvider: an Answer pattern for relations
		GraphPattern gp1 = new GraphPattern(prefixes, "?a ex:isRelatedTo ?b .");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1, "answerRelations");
		kbRelationProvider.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			// add 1 dummy binding to the answer
			BindingSet bindingSet = new BindingSet();
			Binding binding1 = new Binding();
			binding1.put("a", "<https://www.tno.nl/example/Barry>");
			binding1.put("b", "<https://www.tno.nl/example/Jack>");
			bindingSet.add(binding1);

			return bindingSet;
		});
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestAskAnswerWithGapsEnabled2.class.getSimpleName());
		kn.stop().get();
	}
}
