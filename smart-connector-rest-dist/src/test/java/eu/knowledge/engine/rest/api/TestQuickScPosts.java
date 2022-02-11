package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.AsyncTester;
import eu.knowledge.engine.test_utils.HttpTester;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestQuickScPosts {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() throws InterruptedException {
		rsh.start(PORT);
	}

	@Test
	public void testPostScsSimultaneously() throws IOException {
		URL url = new URL("http://localhost:" + PORT + "/rest/sc");

		// You can make the test pass consistently by uncommenting the following:
		// new HttpTester(url, "GET", null, Map.of(
		// 	"Accept", "*/*"
		// )).expectStatus(200);

		// This latch is used to make the threads synchronize before they will
		// register. This way, the registration requests are likely to be very close
		// to eachother, allowing us to reproduce the error.
		final var beforeRegister = new CountDownLatch(2);

		// This latch is used to make sure that both KBs are registered before
		// attempting to assert their existence.
		final var afterRegister = new CountDownLatch(2);

		String kb1Id = "http://example.org/kb1";
		String kb2Id = "http://example.org/kb2";

		var postingSc1 = new AsyncTester(new Runnable() {
			@Override
			public void run() {
				beforeRegister.countDown();
				try {
					beforeRegister.await();
				} catch (InterruptedException e) {
					fail();
				}

				new HttpTester(url, "POST",
					"{\"knowledgeBaseId\": \""+ kb1Id + "\", \"knowledgeBaseName\": \"KB1\", \"knowledgeBaseDescription\": \"KB1\"}",
					Map.of(
						"Content-Type", "application/json",
						"Accept", "*/*"
					)
				).expectStatus(200);

				afterRegister.countDown();
				try {
					afterRegister.await();
				} catch (InterruptedException e) {
					fail();
				}

				new HttpTester(url, "GET", null, Map.of(
					"Accept", "*/*",
					"Knowledge-Base-Id", kb1Id
				)).expectStatus(200);

				new HttpTester(url, "GET", null, Map.of(
					"Accept", "*/*",
					"Knowledge-Base-Id", kb2Id
				)).expectStatus(200);
			}
		});

		var postingSc2 = new AsyncTester(new Runnable() {
			@Override
			public void run() {
				beforeRegister.countDown();
				try {
					beforeRegister.await();
				} catch (InterruptedException e) {
					fail();
				}

				new HttpTester(url, "POST",
					"{\"knowledgeBaseId\": \""+ kb2Id + "\", \"knowledgeBaseName\": \"KB1\", \"knowledgeBaseDescription\": \"KB1\"}",
					Map.of(
						"Content-Type", "application/json",
						"Accept", "*/*"
					)
				).expectStatus(200);
				
				afterRegister.countDown();
				try {
					afterRegister.await();
				} catch (InterruptedException e) {
					fail();
				}

				new HttpTester(url, "GET", null, Map.of(
					"Accept", "*/*",
					"Knowledge-Base-Id", kb1Id
				)).expectStatus(200);

				new HttpTester(url, "GET", null, Map.of(
					"Accept", "*/*",
					"Knowledge-Base-Id", kb2Id
				)).expectStatus(200);
			}
		});

		// Start both threads
		postingSc1.start();
		postingSc2.start();

		postingSc1.joinAndRethrow();
		postingSc2.joinAndRethrow();
	}

	@AfterAll
	public void cleanUp() {
		rsh.cleanUp();
	}
}
