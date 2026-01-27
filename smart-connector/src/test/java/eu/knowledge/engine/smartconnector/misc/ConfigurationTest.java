package eu.knowledge.engine.smartconnector.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
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
import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class ConfigurationTest {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTest.class);

	private KnowledgeNetwork kn;
	private KnowledgeBaseImpl kb1;
	private AskKnowledgeInteraction askKI;
	private KnowledgeBaseImpl kb2;
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
		LOG.info("Test: testConfigValidateTrue");
		System.setProperty(SmartConnectorConfig.CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS, "true");
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

		System.clearProperty(SmartConnectorConfig.CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS);
	}

	@Test
	public void testConfigValidateFalse() {
		LOG.info("Test: testConfigValidateFalse");
		System.setProperty(SmartConnectorConfig.CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS, "false");

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
		System.clearProperty(SmartConnectorConfig.CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS);
	}

	@Test
	public void testConfigWaitForKnowledgeBaseNegative() {
		LOG.info("Test: testConfigWaitForKnowledgeBaseNegative");
		System.setProperty(SmartConnectorConfig.CONF_KEY_KE_KB_WAIT_TIMEOUT, "1");
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
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KE_KB_WAIT_TIMEOUT);
	}

	@Test
	public void testConfigWaitForKnowledgeBasePositive() {
		LOG.info("Test: testConfigWaitForKnowledgeBasePositive");
		System.setProperty(SmartConnectorConfig.CONF_KEY_KE_KB_WAIT_TIMEOUT, "2");
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
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KE_KB_WAIT_TIMEOUT);
	}

	@Test
	public void testConfigReasonerLevelOutOfRange() {
		LOG.info("Test: testConfigReasonerLevelOutOfRange");
		var kb = new KnowledgeBaseImpl("kb11");
		this.kn.addKB(kb);
		kb.setReasonerLevel(0);
		assertThrowsExactly(IllegalArgumentException.class, () -> kb.start());
		LOG.info("after first");

		var kbb = new KnowledgeBaseImpl("kb22");
		this.kn.addKB(kbb);
		kbb.setReasonerLevel(6);
		assertThrowsExactly(IllegalArgumentException.class, () -> kbb.start());

		// less elegant solution to wait for SCs to be ready. KnowledgeNetwork solution
		// did not work due to illegal argument exception
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			LOG.info("{}", e);
		}

	}

	@AfterEach
	public void afterTest() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", ConfigurationTest.class.getSimpleName());
		kn.stop().get();
	}

	private void intializeKB1() {
		this.kb1 = new KnowledgeBaseImpl("kb1");
		GraphPattern gp1 = new GraphPattern("""
				?s a <Person> .
				?s <hasName> ?n .
				""");
		this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp1);
		this.kb1.register(askKI);
	}

	private void intializeKB2() {
		this.kb2 = new KnowledgeBaseImpl("kb2");
		GraphPattern gp1 = new GraphPattern("""
				?p a <Person> .
				?p <hasName> ?name .
				""");
		this.answerKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		this.kb2.register(answerKI, (AnswerKnowledgeInteraction ki, AnswerExchangeInfo exchangeInfo) -> {
			var bs = new BindingSet();
			var b = new Binding();
			b.put("p", "<barry1>");
			b.put("name", "\"Barry NL\"");
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
