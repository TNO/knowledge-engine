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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestAskAnswerWithGapsEnabled1 {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerWithGapsEnabled1.class);

	private static KnowledgeBaseImpl kbRelationAsker;
	
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
		
		// In this test there will be only 1 KB with a single AskKI.
		// The test will execute the AskKI with knowledge gaps enabled.
		// As a result, the set of knowledge gaps should contain a single gap.

		setupNetwork();
		
		// Perform the ASK
		try {
			AskResult result = kbRelationAsker.ask(askKIGaps, new BindingSet()).get();
			// check whether set of knowledge gaps contains a single gap
			Set<KnowledgeGap> gaps = result.getKnowledgeGaps();		
			LOG.info("Found gaps: " + gaps);
			assertFalse(gaps.isEmpty(),"The set of knowledge gaps should be empty");
			assertEquals(1, gaps.size(), "Number of gaps should be 1");
			Iterator<KnowledgeGap> iter = gaps.iterator();
			while (iter.hasNext()) {
				KnowledgeGap gap = iter.next();
				assertEquals("[?a isRelatedTo ?b]", gap.toString(), "Gap should be [?a isRelatedTo ?b]");
			}
			BindingSet bindings = result.getBindings();
			LOG.info("Resulting binding set is: " + bindings);
			assertTrue(bindings.isEmpty(),"The resulting bindingset should be empty");
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
		
	}

	private void setupNetwork() {

		instantiateAskRelationsKB();
		kn = new KnowledgeNetwork();
		kn.addKB(kbRelationAsker);

		long start = System.nanoTime();
		kn.sync();
		long end = System.nanoTime();
		LOG.info("Duration: {}", (((double) (end - start)) / 1_000_000));
	}

	public void instantiateAskRelationsKB() {
		// start a knowledge base with the behavior "I am interested in related people"
		kbRelationAsker = new KnowledgeBaseImpl("RelationAsker");
		kbRelationAsker.setReasonerEnabled(true);
		
		// Register an Ask pattern for relations with knowledge gaps enabled
		GraphPattern gp1 = new GraphPattern(prefixes, "?a ex:isRelatedTo ?b .");
		this.askKIGaps = new AskKnowledgeInteraction(new CommunicativeAct(), gp1, "askRelations", false, true, true, MatchStrategy.SUPREME_LEVEL);
		kbRelationAsker.register(this.askKIGaps);

	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestAskAnswerWithGapsEnabled1.class.getSimpleName());
		kn.stop().get();
	}
}
