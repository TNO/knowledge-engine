package eu.knowledge.engine.smartconnector.api;

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

public class TestAskPartialMetadata {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskPartialMetadata.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;

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
		kb3 = new KnowledgeBaseImpl("kb3");
		kn.addKB(kb3);

		GraphPattern gp2 = new GraphPattern(prefixes,
				"?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase>.",
				"?x <https://w3id.org/knowledge-engine/hasName> \"kb1\" ");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2, true);
		kb2.register(askKI);

		kn.sync();

		// start testing!
		BindingSet bindings = null;
		try {
			LOG.info("Before ask.");
			AskResult result = kb2.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.info("After ask: {}", bindings);
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertTrue(b.containsKey("x"), "The variable should be bound.");
		assertTrue(b.get("x").equals("<https://www.tno.nl/kb1>"));
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestAskPartialMetadata.class.getSimpleName());
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
