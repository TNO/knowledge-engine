package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.interconnectproject.knowledge_engine.knowledgedirectory.KnowledgeDirectory;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.kd.model.KnowledgeEngineRuntime;

public class KnowledgeDirectoryConnectionManagerTest {

	@Test
	public void testSuccess() throws Exception {
		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		kd.start();

		KnowledgeDirectoryConnectionManager cm = new KnowledgeDirectoryConnectionManager("localhost", 8080, "localhost",
				8081);

		assertEquals(KnowledgeDirectoryConnectionManager.State.UNREGISTERED, cm.getState());

		Thread.sleep(5000);

		cm.start();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.REGISTERED, cm.getState());
		List<KnowledgeEngineRuntime> knowledgeEngineRuntimes = cm.getKnowledgeEngineRuntimes();
		assertEquals(1, knowledgeEngineRuntimes.size());
		assertEquals("localhost", knowledgeEngineRuntimes.get(0).getHostname());
		assertEquals(8081, knowledgeEngineRuntimes.get(0).getPort());
		assertEquals(cm.getIdAtKnowledgeDirectory(), knowledgeEngineRuntimes.get(0).getId());

		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.STOPPED, cm.getState());

		kd.stop();
	}

	@Test
	public void testNoKd() throws Exception {
		KnowledgeDirectoryConnectionManager cm = new KnowledgeDirectoryConnectionManager("localhost", 8080, "localhost",
				8081);

		assertEquals(KnowledgeDirectoryConnectionManager.State.UNREGISTERED, cm.getState());

		cm.start();

		Thread.sleep(5000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.INTERRUPTED, cm.getState());

		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.STOPPED, cm.getState());
	}

	@Test
	public void testInterrupted() throws Exception {
		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		kd.start();

		KnowledgeDirectoryConnectionManager cm = new KnowledgeDirectoryConnectionManager("localhost", 8080, "localhost",
				8081);

		assertEquals(KnowledgeDirectoryConnectionManager.State.UNREGISTERED, cm.getState());

		Thread.sleep(5000);

		cm.start();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.REGISTERED, cm.getState());

		kd.stop();

		// Wait for the cm to discover the kd is gone
		Thread.sleep(40000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.INTERRUPTED, cm.getState());

		// Restart the KD
		kd = new KnowledgeDirectory(8080);
		kd.start();

		Thread.sleep(35000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.REGISTERED, cm.getState());

		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnectionManager.State.STOPPED, cm.getState());

		kd.stop();

	}

}
