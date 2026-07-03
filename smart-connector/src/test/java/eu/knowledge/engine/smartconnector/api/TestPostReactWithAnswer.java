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

/**
 * We test a KB that posts knowledge that is received by another KB, but only if
 * it is supplemented with data from another KB. We check whether the exchange
 * info is correct and includes both the post and ask exchange infos.
 */
public class TestPostReactWithAnswer {
	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;
	private static KnowledgeBaseImpl kb3;

	public boolean kb2Received = false;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReactWithAnswer.class);
	private static KnowledgeNetwork kn;

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.example.org/example/");

		kn = new KnowledgeNetwork();
		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);
		kb3 = new KnowledgeBaseImpl("kb3");
		kn.addKB(kb3);

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.example.org/example/b> ?c.");
		PostKnowledgeInteraction pKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null);
		kb1.register(pKI);

		GraphPattern gp2 = new GraphPattern(prefixes,
				"?d <https://www.example.org/example/b> ?e. ?d <https://www.example.org/example/f> ?g .");
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp2, null);
		kb2.register(rKI, ((anRKI, aReactExchangeInfo) -> {
			LOG.trace("KB2 reacting...");
			TestPostReactWithAnswer.this.kb2Received = true;
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			assertTrue(iter.hasNext(), "There should be at least a single binding.");
			Binding b = iter.next();

			assertEquals("<https://www.example.org/example/a>", b.get("d"), "Binding of 'd' is incorrect.");
			assertEquals("<https://www.example.org/example/c>", b.get("e"), "Binding of 'e' is incorrect.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

			return new BindingSet();
		}));

		GraphPattern gp3 = new GraphPattern(prefixes, "?a <https://www.example.org/example/f> ?b .");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3);
		kb3.register(aKI, (_, _) -> {
			BindingSet bs = new BindingSet();
			Binding b = new Binding();
			b.put("a", "<https://www.example.org/example/a>");
			b.put("b", "<https://www.example.org/example/z>");
			bs.add(b);

			return bs;
		});

		kn.sync();
		LOG.info("Everyone is up-to-date!");

		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("a", "<https://www.example.org/example/a>");
		binding.put("c", "<https://www.example.org/example/c>");
		bindingSet.add(binding);

		try {
			PostResult result = kb1.post(pKI, bindingSet).get();
			assertTrue(this.kb2Received, "KB2 should have received the posted data.");
			assertEquals(2, result.getExchangeInfoPerKnowledgeBase().size());

			BindingSet bs = result.getBindings();
			assertTrue(bs.isEmpty());

			LOG.info("After post!");
		} catch (ExecutionException e) {
			LOG.error("Error", e);
			fail();
		}
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestPostReactWithAnswer.class.getSimpleName());
		kn.stop().get();
	}

}
