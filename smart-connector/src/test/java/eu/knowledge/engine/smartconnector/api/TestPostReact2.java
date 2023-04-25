package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class TestPostReact2 {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;
	private static MockedKnowledgeBase kb3;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReact2.class);

	public boolean kb2Received = false, kb3Received = false;

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		KnowledgeNetwork kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kn.addKB(kb2);
		kb3 = new MockedKnowledgeBase("kb3");
		kn.addKB(kb3);

		// start registering
		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		PostKnowledgeInteraction ki1 = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null);
		kb1.register(ki1);

		GraphPattern gp2 = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		ReactKnowledgeInteraction ki2 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp2, null);
		kb2.register(ki2, (anRKI, aReactExchangeInfo) -> {

			LOG.info("KB2 Reacting...");

			TestPostReact2.this.kb2Received = true;

			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be at least a single binding.");
			Binding b = iter.next();

			assertEquals("<https://www.tno.nl/example/a>", b.get("d"), "Binding of 'd' should be correct.");
			assertEquals("<https://www.tno.nl/example/c>", b.get("e"), "Binding of 'e' should be correct.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			return new BindingSet();
		});

		GraphPattern gp3 = new GraphPattern(prefixes, "?f <https://www.tno.nl/example/b> ?g");
		ReactKnowledgeInteraction ki3 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3, null);
		kb3.register(ki3, (anRKI, aReactExchangeInfo) -> {

			LOG.info("KB3 Reacting...");
			TestPostReact2.this.kb3Received = true;
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			Binding b = iter.next();

			assertEquals("<https://www.tno.nl/example/a>", b.get("f"), "Binding of 'f' should be correct.");
			assertEquals("<https://www.tno.nl/example/c>", b.get("g"), "Binding of 'g' should be correct.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			return new BindingSet();
		});

		kn.sync();
		LOG.info("Everyone is up-to-date!");

		// start exchanging
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("a", "<https://www.tno.nl/example/a>");
		binding.put("c", "<https://www.tno.nl/example/c>");
		bindingSet.add(binding);

		try {
			PostResult result = kb1.post(ki1, bindingSet).get();

			assertTrue(this.kb2Received, "KB2 should have received the posted data.");
			assertTrue(this.kb3Received, "KB3 should have received the posted data.");
			BindingSet bs = result.getBindings();
			assertTrue(bs.isEmpty());

			LOG.info("After post!");
		} catch (Exception e) {
			LOG.error("Erorr", e);
		}
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestPostReact2.class.getSimpleName());
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
