package eu.knowledge.engine.smartconnector.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import eu.knowledge.engine.smartconnector.api.ExchangeInfo;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

public class ConfigurationTest {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTest.class);

	private KnowledgeNetwork kn;
	private MockedKnowledgeBase kb1;
	private AskKnowledgeInteraction askKI;
	private MockedKnowledgeBase kb2;
	private AnswerKnowledgeInteraction answerKI;
	private int waitTimeout = 0;

	@BeforeEach
	public void beforeTest() {
		this.kn = new KnowledgeNetwork();

		intializeKB1();
		intializeKB2();
		kn.addKB(kb1);
		kn.addKB(kb2);
		kn.sync();
	}

	@Test
	public void testConfigValidateTrue() {
		System.setProperty("sc.validate.outgoing.bindings.wrt.incoming.bindings", "true");
		BindingSet bs = new BindingSet();

		var bs1 = new Binding();
		bs1.put("s", "<barry2>");
		bs.add(bs1);

		var future = kb1.ask(this.askKI, bs);

		AskResult askResult = null;
		try {
			askResult = future.get();
			assertTrue(askResult.getExchangeInfoPerKnowledgeBase().iterator().hasNext());
			var info = askResult.getExchangeInfoPerKnowledgeBase().iterator().next();

			assertEquals(ExchangeInfo.Status.FAILED, info.getStatus());
			assertTrue(info.getFailedMessage().contains("java.lang.IllegalArgumentException"));

		} catch (InterruptedException | ExecutionException e) {
			LOG.info("{}", e);
		}

		LOG.info("Result: {}", askResult);
	}

	@Test
	public void testConfigValidateFalse() {
		System.setProperty("sc.validate.outgoing.bindings.wrt.incoming.bindings", "false");

		BindingSet bs = new BindingSet();

		var bs1 = new Binding();
		bs1.put("s", "<barry2>");
		bs.add(bs1);

		var future = kb1.ask(this.askKI, bs);

		AskResult askResult = null;
		try {
			askResult = future.get();
			assertTrue(askResult.getExchangeInfoPerKnowledgeBase().iterator().hasNext());
			var info = askResult.getExchangeInfoPerKnowledgeBase().iterator().next();

			assertEquals(ExchangeInfo.Status.SUCCEEDED, info.getStatus());
			assertEquals(null, info.getFailedMessage());

		} catch (InterruptedException | ExecutionException e) {
			LOG.info("{}", e);
		}

		LOG.info("Result: {}", askResult);
	}

	@Test
	public void testConfigWaitForKnowledgeBaseNegative() {
		System.setProperty("ke.kb.wait.timeout", "1");
		waitTimeout = 2000;

		BindingSet bs = new BindingSet();

		var bs1 = new Binding();
		bs1.put("s", "<barry1>");
		bs.add(bs1);

		var future = kb1.ask(this.askKI, bs);

		AskResult askResult = null;
		try {
			askResult = future.get();
			assertTrue(askResult.getExchangeInfoPerKnowledgeBase().iterator().hasNext());
			var info = askResult.getExchangeInfoPerKnowledgeBase().iterator().next();

			assertEquals(ExchangeInfo.Status.FAILED, info.getStatus());
			assertTrue(info.getFailedMessage() != null);
			assertTrue(info.getFailedMessage().contains("TimeoutException"));

		} catch (InterruptedException | ExecutionException e) {
			LOG.info("{}", e);
		}

		LOG.info("Result: {}", askResult);
		waitTimeout = 0;
	}

	@Test
	public void testConfigWaitForKnowledgeBasePositive() {
		System.setProperty("ke.kb.wait.timeout", "2");
		waitTimeout = 0;
		BindingSet bs = new BindingSet();

		var bs1 = new Binding();
		bs1.put("s", "<barry1>");
		bs.add(bs1);

		var future = kb1.ask(this.askKI, bs);

		AskResult askResult = null;
		try {
			askResult = future.get();
			assertTrue(askResult.getExchangeInfoPerKnowledgeBase().iterator().hasNext());
			var info = askResult.getExchangeInfoPerKnowledgeBase().iterator().next();

			assertEquals(ExchangeInfo.Status.SUCCEEDED, info.getStatus());
			assertEquals(null, info.getFailedMessage());

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

			try {
				Thread.sleep(waitTimeout);
			} catch (InterruptedException e) {
				fail();
			}

			return bs;
		});
	}

}
