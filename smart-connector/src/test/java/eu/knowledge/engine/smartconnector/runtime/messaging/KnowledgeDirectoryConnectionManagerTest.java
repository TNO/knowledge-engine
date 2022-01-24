package eu.knowledge.engine.smartconnector.runtime.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.knowledgedirectory.KnowledgeDirectory;
import eu.knowledge.engine.smartconnector.runtime.messaging.KnowledgeDirectoryConnection;
import eu.knowledge.engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

public class KnowledgeDirectoryConnectionManagerTest {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeDirectoryConnectionManagerTest.class);

	@Test
	public void testSuccess() throws Exception {
		assertTrue(NetUtils.portAvailable(8080));
		KnowledgeDirectory kd = new KnowledgeDirectory(8080);
		kd.start();
		
		KnowledgeDirectoryConnection cm = new KnowledgeDirectoryConnection("localhost", 8080, new URI("http://localhost:8081"));
		
		assertEquals(KnowledgeDirectoryConnection.State.UNREGISTERED, cm.getState());
		
		Thread.sleep(5000);
		
		assertFalse(NetUtils.portAvailable(8080));
		cm.start();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.REGISTERED, cm.getState());
		List<KnowledgeEngineRuntimeConnectionDetails> kerConnectionDetails = cm
				.getKnowledgeEngineRuntimeConnectionDetails();
		assertEquals(1, kerConnectionDetails.size());
		assertEquals("http://localhost:8081", kerConnectionDetails.get(0).getExposedUrl().toString());
		assertEquals(cm.getMyKnowledgeDirectoryId(), kerConnectionDetails.get(0).getId());

		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.STOPPED, cm.getState());

		kd.stop();
		assertTrue(NetUtils.portAvailable(8080));
	}

	@Test
	public void testNoKd() throws Exception {

		KnowledgeDirectoryConnection cm = new KnowledgeDirectoryConnection("localhost", 8080, new URI("http://localhost:8081"));

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
		assertTrue(NetUtils.portAvailable(8080));
		KnowledgeDirectoryConnection cm = null;
		KnowledgeDirectory kd = null;
		kd = new KnowledgeDirectory(8080);
		kd.start();

		cm = new KnowledgeDirectoryConnection("localhost", 8080, new URI("http://localhost:8081"));

		assertEquals(KnowledgeDirectoryConnection.State.UNREGISTERED, cm.getState());

		Thread.sleep(5000);

		cm.start(); // CM registers itself and starts the lease (60 S)...

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.REGISTERED, cm.getState());

		kd.stop();

		// Wait for the CM to discover the KD is gone
		Thread.sleep(40000);

		// CM tries to renew every 33 seconds, so at this point it has tried that,
		// and found out that the KD has disappeared. This changes the state of the
		// CM so it will now try to REGISTER itself 33 seconds from now.

		assertEquals(KnowledgeDirectoryConnection.State.INTERRUPTED, cm.getState());

		// Restart the KD (as far as we know now, it will retain the list of
		// runtimes with their expirations, because it is restarted in the same JVM,
		// and a static object is used...)
		assertTrue(NetUtils.portAvailable(8080));
		kd = new KnowledgeDirectory(8080);
		kd.start();

		// At this time the lease of CM is not yet expired (only 41 seconds (+ a
		// bit) has passed.)

		Thread.sleep(35000);

		// During the above sleep, another 33 seconds have passed, so the CM will
		// have tried to REGISTER with the new KD. By that time, the lease will have
		// expired (since we reuse the list of runtimes), and it will be rejected
		// first, but then re-register.

		assertEquals(KnowledgeDirectoryConnection.State.REGISTERED, cm.getState());
		cm.stop();

		Thread.sleep(1000);

		assertEquals(KnowledgeDirectoryConnection.State.STOPPED, cm.getState());
		kd.stop();
		assertTrue(NetUtils.portAvailable(8080));
	}

}
