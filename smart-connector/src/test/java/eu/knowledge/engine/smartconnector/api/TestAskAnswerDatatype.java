package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.ExecutionException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

class TestAskAnswerDatatype {

	private static final Logger LOG = LoggerFactory.getLogger(TestAskAnswerDatatype.class);

	@Test
	void test() throws InterruptedException, ExecutionException {

		System.setProperty("ke.kb.wait.timeout", "0");

		KnowledgeNetwork kn = new KnowledgeNetwork();

		KnowledgeBaseImpl kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);
		KnowledgeBaseImpl kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

		PrefixMapping pm = new PrefixMappingMem();
		pm.setNsPrefix("ex", "https://www.example.com/");

		GraphPattern gp = new GraphPattern(pm,
				"?input ex:hasValue ?value . ?input ex:hasTestValue ?testValue . ?input ex:hasReverseResult ?reverseResult .");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
		kb1.register(askKI);

		AnswerKnowledgeInteraction answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp);
		kb2.register(answerKI, (aKI, exchangeInfo) -> {

			BindingSet incomingBS = exchangeInfo.getIncomingBindings();

			LOG.info("Incoming bs: {}", incomingBS);

			BindingSet bs = new BindingSet();

			Binding b1 = new Binding();
			b1.put("value", "\"My string value\"^^<http://www.w3.org/2001/XMLSchema#string>");
			b1.put("testValue", "\"7\"^^<http://www.w3.org/2001/XMLSchema#int>");
			b1.put("input", "<https://www.example.com/inputx>");
			b1.put("reverseResult", "\"eulav gnirts yM\"^^<http://www.w3.org/2001/XMLSchema#string>");
			bs.add(b1);
			return bs;
		});

		kn.sync();

		BindingSet bs = new BindingSet();
		Binding b1 = new Binding();
		b1.put("value", "\"My string value\"^^<http://www.w3.org/2001/XMLSchema#string>");
		b1.put("testValue", "\"7\"^^<http://www.w3.org/2001/XMLSchema#int>");
		bs.add(b1);

		AskResult askResult = kb1.ask(askKI, bs).get();

		LOG.info("AskResult: {}", askResult);

		assert askResult.getBindings().size() > 0 : "There should be bindings returned.";

	}

}
