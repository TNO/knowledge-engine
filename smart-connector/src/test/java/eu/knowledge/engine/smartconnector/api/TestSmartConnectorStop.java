package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

public class TestSmartConnectorStop {

	private static final Logger LOG = LoggerFactory.getLogger(TestSmartConnectorStop.class);

	private static SmartConnector sc1;
	private static SmartConnector sc2;
	private static AtomicBoolean stopped = new AtomicBoolean(false);

	@Test
	public void test() throws InterruptedException {

		sc1 = SmartConnectorBuilder.newSmartConnector(new MyKnowledgeBase("kb1")).create();
		sc2 = SmartConnectorBuilder.newSmartConnector(new MyKnowledgeBase("kb2")).create();

		sc1.register(new AskKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?t1 <relatesTo> ?t2 .")));

		sc2.register(new AnswerKnowledgeInteraction(new CommunicativeAct(), new GraphPattern("?r1 <relatesTo> ?r2 .")),
				(AnswerKnowledgeInteraction var1, AnswerExchangeInfo var2) -> {

					return new BindingSet();
				});

		Thread.sleep(2000);

		sc1.stop();

		assertTrue(stopped.get());

		Thread.sleep(2000);
	}

	class MyKnowledgeBase implements KnowledgeBase {

		private String name;

		//@formatter:off
		public MyKnowledgeBase(String aName) { this.name = aName; }
		
		@Override
		public URI getKnowledgeBaseId() { return URI.create("http://example.org/" + this.name);	}

		@Override
		public String getKnowledgeBaseName() { return this.name + " name"; }

		@Override
		public String getKnowledgeBaseDescription() { return this.name + " desc"; }

		@Override
		public void smartConnectorReady(SmartConnector aSC) { LOG.info("{} ready!", this.name); }

		@Override
		public void smartConnectorConnectionLost(SmartConnector aSC) { LOG.info("{} lost!", this.name); }

		@Override
		public void smartConnectorConnectionRestored(SmartConnector aSC) { LOG.info("{} restored!", this.name); }

		@Override
		public void smartConnectorStopped(SmartConnector aSC) { TestSmartConnectorStop.stopped.set(true); LOG.info("{} stopped!", this.name); }
		//@formatter:on
	}

}
