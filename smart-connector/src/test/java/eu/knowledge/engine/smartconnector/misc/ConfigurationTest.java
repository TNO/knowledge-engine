package eu.knowledge.engine.smartconnector.misc;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.AnswerExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class ConfigurationTest {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTest.class);

	public KnowledgeNetwork kn;
	public MockedKnowledgeBase kb1;
	public AskKnowledgeInteraction askKI;
	public MockedKnowledgeBase kb2;
	public AnswerKnowledgeInteraction answerKI;

	@BeforeEach
	public void beforeTest() {
		this.kn = new KnowledgeNetwork();

		intializeKB1();
		intializeKB2();
		kn.addKB(kb1);
		kn.addKB(kb2);

		LOG.info("Before sync...");
		kn.sync();
		LOG.info("After sync...");
	}

	@Test
	public void testConfigValidateDefault() {
		System.setProperty("sc.validate.outgoing.bindings.wrt.incoming.bindings", "true");

		BindingSet bs = new BindingSet();

		var bs1 = new Binding();
		bs1.put("s", "<barry2>");
		bs.add(bs1);

		var future = kb1.ask(this.askKI, bs);

		AskResult askResult = null;
		try {
			askResult = future.get();
			// TODO check for FAILED and Illegal argument in exchange info.

		} catch (InterruptedException | ExecutionException e) {
			LOG.info("{}", e);
		}

		LOG.info("Result: {}", askResult);
	}

	@AfterEach
	public void afterTTest() {
		try {
			kn.stop().get();
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	private void intializeKB1() {
		this.kb1 = new MockedKnowledgeBase("kb1");
		GraphPattern gp1 = new GraphPattern("""
				?s a <Person> .
				?s <hasName> ?n .
				""");
		this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp1);
		this.kb1.register(askKI);
	}

	private void intializeKB2() {
		this.kb2 = new MockedKnowledgeBase("kb2");
		GraphPattern gp1 = new GraphPattern("""
				?p a <Person> .
				?p <hasName> ?name .
				""");
		this.answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		this.kb2.register(answerKI, (AnswerKnowledgeInteraction ki, AnswerExchangeInfo exchangeInfo) -> {
			var bs = new BindingSet();
			var b = new Binding();
			b.put("p", "<barry1>");
			b.put("name", "\"Barry Nouwt\"");
			bs.add(b);
			return bs;
		});
	}

}
