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

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestPostReact5 {
	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;

	public boolean kb2Received = false;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReact5.class);

	@Test
	public void testPostReactTimeout() throws InterruptedException {
		System.setProperty(SmartConnectorConfig.CONF_KEY_KE_KB_WAIT_TIMEOUT, "1");
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		KnowledgeNetwork kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		GraphPattern gp2 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/d> ?c.");
		PostKnowledgeInteraction pKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, gp2);
		kb1.register(pKI);

		GraphPattern gp3 = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		GraphPattern gp4 = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/d> ?e.");
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3, gp4);
		kb2.register(rKI, ((anRKI, aReactExchangeInfo) -> {
			LOG.trace("KB2 reacting...");
			TestPostReact5.this.kb2Received = true;
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be at least a single binding.");
			Binding b = iter.next();

			assertEquals("<https://www.tno.nl/example/a>", b.get("d"), "Binding of 'd' is incorrect.");
			assertEquals("<https://www.tno.nl/example/c>", b.get("e"), "Binding of 'e' is incorrect.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				LOG.error("{}", e);
			}

			BindingSet bindingSet = new BindingSet();
			Binding bind = new Binding();
			bind.put("d", b.get("d"));
			bind.put("e", b.get("e"));
			bindingSet.add(bind);

			return bindingSet;
		}));

		kn.sync();
		LOG.info("Everyone is up-to-date!");

		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("a", "<https://www.tno.nl/example/a>");
		binding.put("c", "<https://www.tno.nl/example/c>");
		bindingSet.add(binding);

		try {
			PostResult result = kb1.post(pKI, bindingSet).get();

			assertTrue(this.kb2Received, "KB2 should have received the posted data.");

			assertEquals(result.getExchangeInfoPerKnowledgeBase().iterator().next().getStatus(),
					ExchangeInfo.Status.FAILED);

			BindingSet bs = result.getBindings();
			assertTrue(bs.isEmpty());

			LOG.info("After post!");
		} catch (ExecutionException e) {
			LOG.error("Error", e);
			fail();
		}
	}

	@AfterAll
	public static void cleanup() {
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KE_KB_WAIT_TIMEOUT);
		LOG.info("Clean up: {}", TestPostReact5.class.getSimpleName());
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
	}

}
