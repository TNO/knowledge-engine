package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

@Tag("Long")
public class TestAddingDeletingManySmartConnectors {

	private static final int AMOUNT = 25;
	private Logger LOG = LoggerFactory.getLogger(TestAddingDeletingManySmartConnectors.class);

	@Disabled
	@Test
	void test() throws InterruptedException, ExecutionException {

		KnowledgeNetwork kn = new KnowledgeNetwork();

		LOG.info("Creating SCs");
		for (int i = 0; i < AMOUNT; i++) {
			kn.addKB(new KnowledgeBaseImpl("kb" + i));
		}
		kn.sync();

		LOG.info("Stopping SCs");
		kn.stop().get();

		LOG.info("Creating SCs again");
		for (int i = 0; i < AMOUNT; i++) {
			kn.addKB(new KnowledgeBaseImpl("kb" + i));
		}
		kn.sync();
		LOG.info("Stopping SCs again");
		kn.stop().get();
	}
}
