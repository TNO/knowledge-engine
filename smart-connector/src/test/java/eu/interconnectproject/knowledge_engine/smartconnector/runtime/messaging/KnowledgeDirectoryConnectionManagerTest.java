package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.knowledgedirectory.KnowledgeDirectory;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

public class KnowledgeDirectoryConnectionManagerTest {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeDirectoryConnectionManagerTest.class);

	@Test
	public void testSuccess() throws Exception {

		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		kd.start();

		KnowledgeDirectoryConnection cm = new KnowledgeDirectoryConnection("localhost", 8080, "localhost", 8081);

		assertEquals(KnowledgeDirectoryConnection.State.UNREGISTERED, cm.getState());

		Thread.sleep(5000);

		cm.start();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.REGISTERED, cm.getState());
		List<KnowledgeEngineRuntimeConnectionDetails> kerConnectionDetails = cm
				.getKnowledgeEngineRuntimeConnectionDetails();
		assertEquals(1, kerConnectionDetails.size());
		assertEquals("localhost", kerConnectionDetails.get(0).getHostname());
		assertEquals(8081, kerConnectionDetails.get(0).getPort());
		assertEquals(cm.getMyKnowledgeDirectoryId(), kerConnectionDetails.get(0).getId());

		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.STOPPED, cm.getState());

		kd.stop();
	}

	@Test
	public void testNoKd() throws Exception {

		KnowledgeDirectoryConnection cm = new KnowledgeDirectoryConnection("localhost", 8080, "localhost", 8081);

		assertEquals(KnowledgeDirectoryConnection.State.UNREGISTERED, cm.getState());

		cm.start();

		Thread.sleep(5000);

		assertEquals(KnowledgeDirectoryConnection.State.INTERRUPTED, cm.getState());

		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.STOPPED, cm.getState());
	}

	@Test
	public void testInterrupted() throws Exception {

		KnowledgeDirectoryConnection cm = null;
		KnowledgeDirectory kd = null;
		kd = new KnowledgeDirectory(8080);
		kd.start();

		cm = new KnowledgeDirectoryConnection("localhost", 8080, "localhost", 8081);

		assertEquals(KnowledgeDirectoryConnection.State.UNREGISTERED, cm.getState());

		Thread.sleep(5000);

		cm.start();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.REGISTERED, cm.getState());

		kd.stop();

		// Wait for the cm to discover the kd is gone
		Thread.sleep(40000);

		assertEquals(KnowledgeDirectoryConnection.State.INTERRUPTED, cm.getState());

		// Restart the KD
		kd = new KnowledgeDirectory(8080);
		kd.start();

		Thread.sleep(35000);
		assertEquals(KnowledgeDirectoryConnection.State.REGISTERED, cm.getState());
		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.STOPPED, cm.getState());
		kd.stop();
	}

}
