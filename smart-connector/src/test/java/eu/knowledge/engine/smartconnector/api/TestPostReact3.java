package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.EasyKnowledgeBase;

public class TestPostReact3 {
	private static EasyKnowledgeBase kb1;
	private static EasyKnowledgeBase kb2;
	private static EasyKnowledgeBase kb3;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReact3.class);

	public boolean kb2Received = false, kb3Received = false;

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://example.org/");

		KnowledgeNetwork kn = new KnowledgeNetwork();
		kb1 = new EasyKnowledgeBase("kb1");
		kb1.setReasonerEnabled(true);
		kn.addKB(kb1);
		kb2 = new EasyKnowledgeBase("kb2");
		kb2.setReasonerEnabled(true);
		kn.addKB(kb2);
		kb3 = new EasyKnowledgeBase("kb3");
		kb3.setReasonerEnabled(true);
		kn.addKB(kb3);

		// start registering
		GraphPattern argumentPattern = new GraphPattern(prefixes, "?a ex:pred1 ?b .");
		GraphPattern resultPattern1 = new GraphPattern(prefixes, "?c ex:pred2 ?d .");
		PostKnowledgeInteraction ki1 = new PostKnowledgeInteraction(new CommunicativeAct(), argumentPattern,
				resultPattern1);
		kb1.register(ki1);

		ReactKnowledgeInteraction ki2 = new ReactKnowledgeInteraction(new CommunicativeAct(), argumentPattern,
				resultPattern1);
		kb2.register(ki2, (anRKI, aReactExchangeInfo) -> {
			TestPostReact3.this.kb2Received = true;

			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be at least a single binding.");
			Binding b = iter.next();

			assertEquals("<https://example.org/example/a>", b.get("a"), "Binding of 'a' should be correct.");
			assertEquals("<https://example.org/example/b>", b.get("b"), "Binding of 'b' should be correct.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			var bs = new BindingSet();
			b = new Binding();
			b.put("c", "<https://example.org/example/c1>");
			b.put("d", "<https://example.org/example/d1>");
			bs.add(b);
			return bs;
		});

		GraphPattern resultPattern2 = new GraphPattern(prefixes, "?c ex:pred2 ?d . ?e ex:pred3 ?f .");
		ReactKnowledgeInteraction ki3 = new ReactKnowledgeInteraction(new CommunicativeAct(), argumentPattern,
				resultPattern2);
		kb3.register(ki3, (anRKI, aReactExchangeInfo) -> {

			LOG.info("KB3 Reacting...");
			TestPostReact3.this.kb3Received = true;
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			Binding b = iter.next();

			assertEquals("<https://example.org/example/a>", b.get("a"), "Binding of 'a' should be correct.");
			assertEquals("<https://example.org/example/b>", b.get("b"), "Binding of 'b' should be correct.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			var bs = new BindingSet();
			b = new Binding();
			b.put("c", "<https://example.org/example/c2>");
			b.put("d", "<https://example.org/example/d2>");
			b.put("e", "<https://example.org/example/e2>");
			b.put("f", "<https://example.org/example/f2>");
			bs.add(b);
			return bs;
		});

		kn.sync();
		LOG.info("Everyone is up-to-date!");

		// start exchanging
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("a", "<https://example.org/example/a>");
		binding.put("b", "<https://example.org/example/b>");
		bindingSet.add(binding);

		try {

			PostPlan plan = kb1.planPost(ki1, new RecipientSelector());

			plan.getReasonerPlan().getStore().printGraphVizCode(plan.getReasonerPlan());

			PostResult result = plan.execute(bindingSet).get();

			assertTrue(this.kb2Received, "KB2 should have received the posted data.");
			assertTrue(this.kb3Received, "KB3 should have received the posted data.");
			BindingSet bs = result.getBindings();
			LOG.info("received post results: {}", bs);
			assertTrue(bs.size() == 2);

			BindingSet expected = new BindingSet();
			Binding b = new Binding();
			b.put("c", "<https://example.org/example/c1>");
			b.put("d", "<https://example.org/example/d1>");
			expected.add(b);
			b = new Binding();
			b.put("c", "<https://example.org/example/c2>");
			b.put("d", "<https://example.org/example/d2>");
			expected.add(b);

			assertEquals(expected, bs);
		} catch (Exception e) {
			LOG.error("Error", e);
			fail();
		}
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestPostReact3.class.getSimpleName());
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
