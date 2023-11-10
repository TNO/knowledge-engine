package eu.knowledge.engine.smartconnector.api;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class TestPostReactInetumRealdomen {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;
	private static MockedKnowledgeBase kb3;

	private static final Logger LOG = LoggerFactory.getLogger(TestPostReactInetumRealdomen.class);
	private static KnowledgeNetwork kn;

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		kn = new KnowledgeNetwork();
		kb1 = new MockedKnowledgeBase("kb1");
		kb1.setReasonerEnabled(true);
		kn.addKB(kb1);
		kb2 = new MockedKnowledgeBase("kb2");
		kb2.setReasonerEnabled(true);
		kn.addKB(kb2);
		kb3 = new MockedKnowledgeBase("kb3");
		kb3.setReasonerEnabled(true);
		kn.addKB(kb3);

		// start registering
		GraphPattern gp1 = new GraphPattern(prefixes,
				"?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/Tank> .");
		AskKnowledgeInteraction ki1 = new AskKnowledgeInteraction(new CommunicativeAct(), gp1, "askTank");
		kb1.register(ki1);

		GraphPattern gp21 = new GraphPattern(prefixes,
				"?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/Tank> .");
		GraphPattern gp22 = new GraphPattern(prefixes,
				"?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/WaterTank> .");
		ReactKnowledgeInteraction ki2 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp22, gp21);
		kb2.register(ki2, (anRKI, aReactExchangeInfo) -> {

			LOG.info("KB2 Reacting...");

			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			Binding b = iter.next();

			var bs = new BindingSet();
			bs.add(b);

			return bs;
		});

		GraphPattern gp3 = new GraphPattern(prefixes,
				"?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/WaterTank> .");

		AnswerKnowledgeInteraction ki3 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp3, null);
		kb3.register(ki3, (anRKI, aReactExchangeInfo) -> {

			LOG.info("KB3 Answering...");
			var argument = aReactExchangeInfo.getIncomingBindings();
			Iterator<Binding> iter = argument.iterator();
			Binding b = iter.next();

			var bs = new BindingSet();
			var binding = new Binding();
			binding.put("resource", "<http://example.org/myWaterTank>");
			bs.add(binding);
			return bs;
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
			AskResult result = kb1.ask(ki1, new BindingSet()).get();

			LOG.info("AskResult: {}", result);

			BindingSet bs = result.getBindings();

			LOG.info("BS: {}", bs);
			LOG.info("After post!");
		} catch (Exception e) {
			LOG.error("Erorr", e);
		}
		Thread.sleep(1000);
	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestPostReactInetumRealdomen.class.getSimpleName());

		kn.stop().get();
	}

}
