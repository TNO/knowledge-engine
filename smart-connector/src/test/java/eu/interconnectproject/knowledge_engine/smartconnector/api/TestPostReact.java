package eu.interconnectproject.knowledge_engine.smartconnector.api;

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

public class TestPostReact {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReact.class);

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		int wait = 15;
		final CountDownLatch kb2Initialized = new CountDownLatch(1);
		final CountDownLatch kb2ReceivedKnowledge = new CountDownLatch(1);
		final CountDownLatch kb1ReceivedPostResult = new CountDownLatch(1);

		kb1 = new MockedKnowledgeBase("kb1") {
			private PostKnowledgeInteraction ki;

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
				this.ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null);
				aSC.register(this.ki);

				this.waitForOtherKbAndPostSomething();
			}

			private void waitForOtherKbAndPostSomething() {
				// Wait until KB2 completed its latch.
				try {
					assertTrue(kb2Initialized.await(wait, TimeUnit.SECONDS),
							"kb2 should have been initialized within " + wait + " seconds.");
				} catch (InterruptedException e) {
					fail("Should not throw any exception.");
				}

				BindingSet bindingSet = new BindingSet();
				Binding binding = new Binding();
				binding.put("a", "<https://www.tno.nl/example/a>");
				binding.put("c", "<https://www.tno.nl/example/c>");
				bindingSet.add(binding);

				try {
					Thread.sleep(4000); // we wait with posting until all Smart Connectors are up-to-date.
					PostResult result = this.post(this.ki, bindingSet).get();
					LOG.info("After post!");
					kb1ReceivedPostResult.countDown();
				} catch (Exception e) {
					LOG.error("Erorr", e);
				}
			}
		};
		kb1.start();

		kb2 = new MockedKnowledgeBase("kb2") {
			@Override
			public void smartConnectorReady(SmartConnector aSC) {

				GraphPattern gp = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
				ReactKnowledgeInteraction ki = new ReactKnowledgeInteraction(new CommunicativeAct(), gp, null);

				aSC.register(ki, (ReactHandler) (anRKI, argument) -> {

					LOG.trace("Reacting...");

					Iterator<Binding> iter = argument.iterator();
					Binding b = iter.next();

					assertEquals("<https://www.tno.nl/example/a>", b.get("d"), "Binding of 'd' is incorrect.");
					assertEquals("<https://www.tno.nl/example/c>", b.get("e"), "Binding of 'e' is incorrect.");

					assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

					// Complete the latch to make the test pass.
					kb2ReceivedKnowledge.countDown();

					return new BindingSet();
				});
				kb2Initialized.countDown();
			}
		};
		kb2.start();

		assertTrue(kb2ReceivedKnowledge.await(wait, TimeUnit.SECONDS),
				"KB2 should receive knowledge within " + wait + " seconds.");

		assertTrue(kb1ReceivedPostResult.await(wait, TimeUnit.SECONDS),
				"KB1 should receive PostResult within " + wait + " seconds.");

	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestPostReact.class.getSimpleName());
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
