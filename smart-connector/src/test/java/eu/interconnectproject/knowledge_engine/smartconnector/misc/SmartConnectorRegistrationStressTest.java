package eu.interconnectproject.knowledge_engine.smartconnector.misc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeNetwork;
import eu.interconnectproject.knowledge_engine.smartconnector.api.MockedKnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorBuilder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SmartConnectorRegistrationStressTest {
	private static final Logger LOG = LoggerFactory.getLogger(SmartConnectorRegistrationStressTest.class);

	private KnowledgeNetwork kn;
	private static final int NUM_KBS = 30;

	private static final int TEST_FAIL_THRESHOLD_SECONDS = 10;

	@BeforeAll
	void seedKnowledgeNetwork() {
		kn = new KnowledgeNetwork();
		for (var i = 0; i < NUM_KBS; i++) {
			LOG.info("Starting KB{}", i);
			kn.addKB(new MockedKnowledgeBase("INITIAL-KB" + i));
		}
		kn.startAndWaitForReady();
		kn.waitForUpToDate();
	}

	@Test
	public void testRegisterWhenManySmartConnectorsExist() throws ExecutionException, InterruptedException, URISyntaxException {
		Instant beforeRegistration = Instant.now();

		var future = new CompletableFuture<Void>();

		var testKBUri = new URI("https://www.tno.nl/TEST-KB");

		var kb = new KnowledgeBase() {
			@Override
			public URI getKnowledgeBaseId() { return testKBUri; }

			@Override
			public String getKnowledgeBaseName() { return "TEST KB"; }

			@Override
			public String getKnowledgeBaseDescription() { return "A test KB"; }

			@Override
			public void smartConnectorReady(SmartConnector sc) {
				future.complete(null);
			}

			@Override
			public void smartConnectorConnectionLost(SmartConnector aSC) {}

			@Override
			public void smartConnectorConnectionRestored(SmartConnector aSC) {}

			@Override
			public void smartConnectorStopped(SmartConnector aSC) {}
		};
		
		SmartConnectorBuilder.newSmartConnector(kb).create();

		future.get(); // Waits for the future.

		Instant afterRegistration = Instant.now();

		Duration duration = Duration.between(beforeRegistration, afterRegistration);

		LOG.info("Registration took {} milliseconds.", duration.toMillis());
		assertTrue(duration.toSeconds() < TEST_FAIL_THRESHOLD_SECONDS);
	}
}
