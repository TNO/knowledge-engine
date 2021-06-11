package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;

public class TestAddingDeletingManySmartConnectors {

	private static final int AMOUNT = 20;
	private Logger LOG = LoggerFactory.getLogger(TestAddingDeletingManySmartConnectors.class);

	@Disabled
	@Test
	void test() throws InterruptedException {

		KnowledgeBase[] kb = new KnowledgeBase[AMOUNT];

		SmartConnector[] sc = new SmartConnector[AMOUNT];
		LOG.info("Start creating SCs");
		for (int i = 0; i < AMOUNT; i++) {

			kb[i] = new MyKnowledgeBase("kb" + i);
			sc[i] = SmartConnectorBuilder.newSmartConnector(kb[i]).create();
		}

		Thread.sleep(20000);

		LOG.info("Start stopping SCs");
		for (int i = 0; i < AMOUNT; i++) {
			sc[i].stop();
		}

		Thread.sleep(5000);

		LOG.info("Start creating SCs again");
		sc = new SmartConnector[AMOUNT];
		for (int i = 0; i < AMOUNT; i++) {

			kb[i] = new MyKnowledgeBase("kb" + i);
			sc[i] = SmartConnectorBuilder.newSmartConnector(kb[i]).create();
		}

	}

	private static class MyKnowledgeBase implements KnowledgeBase {

		private String name;

		public MyKnowledgeBase(String name) {
			this.name = name;
		}

		@Override
		public URI getKnowledgeBaseId() {
			try {
				return new URI("http://www.tno.nl/" + name);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public String getKnowledgeBaseName() {
			return name;
		}

		@Override
		public String getKnowledgeBaseDescription() {
			return this.name;
		}

		@Override
		public void smartConnectorReady(SmartConnector aSC) {
			// TODO Auto-generated method stub

		}

		@Override
		public void smartConnectorConnectionLost(SmartConnector aSC) {
			// TODO Auto-generated method stub

		}

		@Override
		public void smartConnectorConnectionRestored(SmartConnector aSC) {
			// TODO Auto-generated method stub

		}

		@Override
		public void smartConnectorStopped(SmartConnector aSC) {
			// TODO Auto-generated method stub

		}

	}

}
