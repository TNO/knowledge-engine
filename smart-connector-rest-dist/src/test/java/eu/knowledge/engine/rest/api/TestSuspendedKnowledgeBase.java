package eu.knowledge.engine.rest.api;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.AsyncTester;
import eu.knowledge.engine.test_utils.HttpTester;
import org.junit.jupiter.api.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestSuspendedKnowledgeBase {
	int NUM_UNHANDLED_REQUESTS = 5;
	CountDownLatch latch = new CountDownLatch(NUM_UNHANDLED_REQUESTS);
	private final RestServerHelper rsh = new RestServerHelper();
	private static final int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testSuspendingKnowledgeBase() throws MalformedURLException {
		URL url = new URL("http://localhost:" + PORT + "/rest");
		HttpTester registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"http://example.com/kb1\", " + "\"knowledgeBaseName\": \"KB1\", "
						+ "\"knowledgeBaseDescription\": \"KB1\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);
		HttpTester registerKb2 = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"http://example.com/kb2\", " + "\"knowledgeBaseName\": \"KB2\", "
						+ "\"knowledgeBaseDescription\": \"KB2\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb2.expectStatus(200);

		var sc1 = new AsyncTester(() -> {
			try {
				HttpTester registerAsk = new HttpTester(new URL(url + "/sc/ki"), "POST",
						"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", "
								+ "\"knowledgeInteractionName\": \"ask\"," + "\"graphPattern\": \"?a ?b ?c.\"}",
						Map.of("Knowledge-Base-Id", "http://example.com/kb1", "Content-Type", "application/json",
								"Accept", "*/*"));
				registerAsk.expectStatus(200);
				System.out.println("Registered Ask");
				HttpTester executeAsk = new HttpTester(new URL(url + "/sc/ask"), "POST", "[{}]",
						Map.of("Knowledge-Base-Id", "http://example.com/kb1", "Knowledge-Interaction-Id",
								"http://example.com/kb1/interaction/ask", "Content-Type", "application/json", "Accept",
								"*/*"));
				System.out.println("Executing Ask");
				var body = executeAsk.getBody();
				System.out.println(body);

				for (int i = 0; i < NUM_UNHANDLED_REQUESTS; i++) {
					System.out.println("Making another ask");
					new AsyncTester(() -> {
						HttpTester executeAsk1;
						try {
							executeAsk1 = new HttpTester(new URL(url + "/sc/ask"), "POST", "[{}]",
									Map.of("Knowledge-Base-Id", "http://example.com/kb1", "Knowledge-Interaction-Id",
											"http://example.com/kb1/interaction/ask", "Content-Type",
											"application/json", "Accept", "*/*"));
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
						System.out.println("Executing Ask");
						latch.countDown();
						var body1 = executeAsk1.getBody();
						System.out.println(body1);
					}).start();
				}
			} catch (MalformedURLException e) {
				System.err.println(e.getMessage());
			}
		});

		var sc2 = new AsyncTester(() -> {
			try {
				HttpTester registerAnswer = new HttpTester(new URL(url + "/sc/ki"), "POST",
						"{\"knowledgeInteractionType\": \"AnswerKnowledgeInteraction\", "
								+ "\"knowledgeInteractionName\": \"answer\"," + "\"graphPattern\": \"?a ?b ?c.\"}",
						Map.of("Knowledge-Base-Id", "http://example.com/kb2", "Content-Type", "application/json",
								"Accept", "*/*"));
				registerAnswer.expectStatus(200);
				System.out.println("Registered Answer");

				HttpTester waitForRequest = new HttpTester(new URL(url + "/sc/handle"), "GET", null,
						Map.of("Knowledge-Base-Id", "http://example.com/kb2", "Content-Type", "application/json",
								"Accept", "*/*"));
				var body = waitForRequest.getBody();
				System.out.println(body);

				System.out.println("Waiting for requests");
				var postAnswer = new HttpTester(new URL(url + "/sc/handle"), "POST", """
						  {
						    "handleRequestId": 1,
						    "bindingSet": [{
						        "a": "<http://www.tno.nl/s1>",
						        "b": "<http://www.tno.nl/s2>",
						        "c": "<http://www.tno.nl/s3>"
						    }]
						  }
						""",
						Map.of("Content-Type", "application/json", "Accept", "*/*", "Knowledge-Base-Id",
								"http://example.com/kb2", "Knowledge-Interaction-Id",
								"http://example.com/kb2/interaction/answer"));
				postAnswer.getBody();
				System.out.println("Posted answer");

				latch.await();

				HttpTester deleteSC = new HttpTester(new URL(url + "/sc/"), "DELETE", "", Map.of("Knowledge-Base-Id",
						"http://example.com/kb2", "Content-Type", "application/json", "Accept", "*/*"));
				deleteSC.expectStatus(200);
			} catch (MalformedURLException e) {
				System.err.println(e.getMessage());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		sc1.start();
		sc2.start();
		sc1.joinAndRethrow();
		sc2.joinAndRethrow();

	}

	@AfterAll
	public void cleanUp() {
		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}
}
