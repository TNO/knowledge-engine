package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class TestPostReactPerformance {
	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReactPerformance.class);
	private static KnowledgeNetwork kn;

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

		// register capabilities
		GraphPattern kb1GP = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		PostKnowledgeInteraction postKI = new PostKnowledgeInteraction(new CommunicativeAct(), kb1GP, null);
		kb1.register(postKI);

		GraphPattern kb2GP = new GraphPattern(prefixes, "?d <https://www.tno.nl/example/b> ?e.");
		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), kb2GP, null);
		long bindingCount = 100;
		long count = 1000;

		kb2.register(reactKI, (ReactHandler) (anRKI, aReactExchangeInfo) -> {
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			int idx = 0;
			while (iter.hasNext()) {
				idx++;
				Binding b = iter.next();
				assertTrue(b.containsKey("d"));
				assertTrue(b.containsKey("e"));
			}
			assertEquals(bindingCount, idx, "This BindingSet should have exactly " + bindingCount + " binding(s).");
			return new BindingSet();
		});

		kn.sync();

		LOG.info("start posting");

		BindingSet bs = new BindingSet();
		Binding b;
		for (int i = 0; i < bindingCount; i++) {
			b = new Binding();
			b.put("a", "<https://www.tno.nl/example/a" + i + ">");
			b.put("c", "<https://www.tno.nl/example/c" + i + ">");
			bs.add(b);
		}

		// exchange data
		long start = System.nanoTime();
		for (int i = 0; i < count; i++) {

			try {
				CompletableFuture<PostResult> futureResult = kb1.post(postKI, bs);
//				futureResult.thenAccept((result) -> {
//					LOG.info("result: {}", result);
//					LOG.info("exchange time: {}ms", result.getTotalExchangeTime().toMillis());
//				});

			} catch (Exception e) {
				LOG.error("Error", e);
				fail();
			}
		}

		long duration = (System.nanoTime() - start) / 1000000;

		// this depends a lot on the machine that runs this code. Not really suitable as
		// a test. We might want to use some other metric where we measure the time for
		// a single post and then multiply that by some number to find the maximum. That
		// would adapt to slower machines.
//		assertTrue(duration < 15000);

		LOG.info("{} posts finsihed in {}ms.", count, duration);
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestPostReactPerformance.class.getSimpleName());
		kn.stop().get();
	}

}
