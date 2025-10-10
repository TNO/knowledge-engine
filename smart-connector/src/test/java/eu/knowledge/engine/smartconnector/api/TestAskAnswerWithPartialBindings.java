package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class TestAskAnswerWithPartialBindings {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerWithPartialBindings.class);

	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeNetwork kn;

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			var aBindingSet = anAnswerExchangeInfo.getIncomingBindings();
			assertTrue(!aBindingSet.isEmpty(), "The incoming binding set should not be empty.");

			LOG.info("Incoming bindingset: {}", aBindingSet);

			BindingSet newBindingSet = new BindingSet();

			Iterator<Binding> bIter = aBindingSet.iterator();
			Binding newBinding;
			while (bIter.hasNext()) {
				newBinding = new Binding();
				Binding b = bIter.next();

				assertTrue(b.containsKey("a"));
				assertFalse(b.containsKey("c"));

				if (b.containsKey("a") && b.get("a").equals("<https://www.tno.nl/example/a>")) {
					newBinding.put("a", "<https://www.tno.nl/example/a>");
					newBinding.put("c", "<https://www.tno.nl/example/c>");
				} else if (b.containsKey("a") && b.get("a").equals("<https://www.tno.nl/example/x>")) {
					newBinding.put("a", "<https://www.tno.nl/example/x>");
					newBinding.put("c", "<https://www.tno.nl/example/y>");
				}
				newBindingSet.add(newBinding);
			}

			return newBindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kb2.register(askKI);

		kn.sync();

		// start testing!
		BindingSet bindings = null;
		try {
			LOG.trace("Before ask.");
			BindingSet bindings2 = new BindingSet();

			Binding b1 = new Binding();
			b1.put("x", "<https://www.tno.nl/example/a>");
			bindings2.add(b1);
			Binding b2 = new Binding();
			b2.put("x", "<https://www.tno.nl/example/x>");
			bindings2.add(b2);

			AskResult result = kb2.ask(askKI, bindings2).get();
			bindings = result.getBindings();
			LOG.info("After ask: {}", bindings);
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();
		assertTrue(iter.hasNext(), "there should be at least 1 binding");

		int count = 0;
		Binding b;
		while (iter.hasNext()) {
			count++;
			b = iter.next();

			assertTrue(!b.containsKey("a") && !b.containsKey("c"),
					"The variable names should follow the graph pattern of the current KB.");

			assertTrue(b.containsKey("x") && b.containsKey("y"));

			if (b.get("x").equals("<https://www.tno.nl/example/a>")) {
				assertTrue(b.get("y").equals("<https://www.tno.nl/example/c>"));
			}

			if (b.get("x").equals("<https://www.tno.nl/example/x>")) {
				assertTrue(b.get("y").equals("<https://www.tno.nl/example/y>"));
			}
		}

		int expCount = 2;
		assertEquals(expCount, count, "This BindingSet should have " + expCount + " binding(s)");
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestAskAnswerWithPartialBindings.class.getSimpleName());
		kn.stop().get();
	}
}
