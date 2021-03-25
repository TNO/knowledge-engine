package eu.interconnectproject.knowledge_engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPostReactPerformance {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReactPerformance.class);

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		// create the network
		KnowledgeNetwork kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kn.addKB(kb2);
		kn.startAndWaitForReady();

		// register capabilities
		GraphPattern kb1GP = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		PostKnowledgeInteraction postKI = new PostKnowledgeInteraction(new CommunicativeAct(), kb1GP, null);
		kb1.register(postKI);

		GraphPattern kb2GP = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), kb2GP, null);

		kb2.register(reactKI, (ReactHandler) (anRKI, argument) -> {
			
			Iterator<Binding> iter = argument.iterator();
			Binding b = iter.next();
			assertTrue(b.containsKey("d"));
			assertTrue(b.containsKey("e"));
			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");
			return new BindingSet();
		});

		kn.waitForUpToDate();

		long count = 1000;
		LOG.info("start posting");
		// exchange data
		long start = System.nanoTime();
		for (int i = 0; i < count; i++) {
			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a" + i + ">");
			binding.put("c", "<https://www.tno.nl/example/c" + i + ">");
			bindingSet.add(binding);

			try {
				CompletableFuture<PostResult> futureResult = kb1.post(postKI, bindingSet);
				futureResult.thenAccept((result) -> {

//					LOG.info("result: {}", result);
					LOG.info("exchange time: {}ms", result.getTotalExchangeTime().toMillis());

				});

			} catch (Exception e) {
				LOG.error("Erorr", e);
			}
		}
		
		LOG.info("{} posts finsihed in {}ms.", count, (System.nanoTime() - start) / 1000000);
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestPostReactPerformance.class.getSimpleName());
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
