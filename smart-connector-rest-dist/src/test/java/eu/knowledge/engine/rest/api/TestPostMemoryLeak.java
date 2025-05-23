package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.AsyncTester;
import eu.knowledge.engine.test_utils.HttpTester;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class TestPostMemoryLeak {
	private static final int SLEEPTIME = 10;
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;
	private URL url;

	@BeforeAll
	public void setUpServer() throws InterruptedException {
		rsh.start(PORT);
	}

	@Test
	public void testManyPosts() throws IOException, InterruptedException {
		url = new URL("http://localhost:" + PORT + "/rest/sc");

		final var reactKBReady = new CountDownLatch(1);

		String kb2Id = "http://example.org/kb2";

		var reactingSc = new AsyncTester(new Runnable() {
			@Override
			public void run() {

				String kiId = "http://example.org/kb2/interaction/reactki";

				try {
					new HttpTester(url, "POST",
							"{\"knowledgeBaseId\": \"" + kb2Id
									+ "\", \"knowledgeBaseName\": \"KB2\", \"knowledgeBaseDescription\": \"KB2\"}",
							Map.of("Content-Type", "application/json", "Accept", "*/*")).expectStatus(200);

					HttpTester registerKi = new HttpTester(new URL(url.toString() + "/ki"), "POST", """
								{
								  "knowledgeInteractionType": "ReactKnowledgeInteraction",
								  "knowledgeInteractionName": "reactki",
								  "argumentGraphPattern": "?a rdf:type ex:Something .",
								  "prefixes": {
								    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
								    "ex": "http://example.org/"
								  }
								}
							""",
							Map.of("Knowledge-Base-Id", kb2Id, "Content-Type", "application/json", "Accept", "*/*"));

					registerKi.expectStatus(200);

					reactKBReady.countDown();

					while (true) {
						var test = new HttpTester(new URL(url.toString() + "/handle"), "GET", null, Map
								.of("Knowledge-Base-Id", kb2Id, "Content-Type", "application/json", "Accept", "*/*"));
						test.expectStatus(200);
//						System.out.println("Body: " + test.getBody());

						if (Math.random() > 0.95) {
							JsonReader jp = Json.createReader(new StringReader(test.getBody()));
							JsonObject jo = jp.readObject();
							int handleRequestId = jo.getInt("handleRequestId");
							JsonObjectBuilder builder = Json.createObjectBuilder();

							builder.add("handleRequestId", handleRequestId);
							builder.add("bindingSet", JsonObject.EMPTY_JSON_ARRAY);
							JsonObject jo2 = builder.build();

							String body = jo2.toString();

							System.out.println("Body2: " + body);

							var test2 = new HttpTester(new URL(url.toString() + "/handle"), "POST", body,
									Map.of("Knowledge-Base-Id", kb2Id, "Knowledge-Interaction-Id", kiId, "Content-Type",
											"application/json", "Accept", "*/*"));
							test2.expectStatus(200);
						}
						try {
							Thread.sleep(SLEEPTIME);
						} catch (InterruptedException e) {
							fail();
						}
					}
				} catch (MalformedURLException e) {
					fail();
				}
			}
		});
		reactingSc.start();

		String kb1Id = "http://example.org/kb1";
		String kiId = "http://example.org/kb1/interaction/postki";

		new HttpTester(url, "POST",
				"{\"knowledgeBaseId\": \"" + kb1Id
						+ "\", \"knowledgeBaseName\": \"KB1\", \"knowledgeBaseDescription\": \"KB1\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*")).expectStatus(200);

		HttpTester registerKi = new HttpTester(new URL(url.toString() + "/ki"), "POST", """
					{
					  "knowledgeInteractionType": "PostKnowledgeInteraction",
					  "knowledgeInteractionName": "postki",
					  "argumentGraphPattern": "?a rdf:type ex:Something .",
					  "prefixes": {
					    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
					    "ex": "http://example.org/"
					  }
					}
				""", Map.of("Knowledge-Base-Id", kb1Id, "Content-Type", "application/json", "Accept", "*/*"));

		registerKi.expectStatus(200);
		reactKBReady.await();

		int counter = 0;

		while (true) {
			counter++;
			Thread.sleep(SLEEPTIME);
			System.out.println("post data");
			new AsyncTester(new Runnable() {
				@Override
				public void run() {
					try {

						var test = new HttpTester(new URL(TestPostMemoryLeak.this.url.toString() + "/post"), "POST", """
									[
									  {
									    "a": "<http://www.tno.nl/s1>"
									  },
									  {
									    "a": "<http://www.tno.nl/s2>"
									  }
									]
								""", Map.of("Content-Type", "application/json", "Accept", "*/*", "Knowledge-Base-Id",
								kb1Id, "Knowledge-Interaction-Id", kiId));
						test.expectStatus(200);
						System.out.println("finshed posting: " + test.getBody());
					} catch (MalformedURLException e) {
						fail();
					}

				}
			}).start();
//			if (counter > 5000)
//				break;
		}

		// Start both threads
//		postingSc.start();

//		postingSc.joinAndRethrow();

	}

	@AfterAll
	public void cleanUp() {
		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}
}
