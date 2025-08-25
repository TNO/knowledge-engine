package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
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
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestAskAnswerWithGapsEnabled {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testAskAnswerWithGaps() throws IOException, InterruptedException {

		// In this test there will be an Ask KB with a single AskKI and
		// an AnswerKB with a single AnswerKI that answers only part of the Ask pattern.
		// The test will execute the AskKI with knowledge gaps enabled.
		// As a result, the set of knowledge gaps should contain a single gap.

		CountDownLatch KBReady = new CountDownLatch(1);

		URL url = new URL("http://localhost:" + PORT + "/rest");

		// activate the answer SC, KB, KI in a separate thread
		var answeringSc = new AsyncTester(new Runnable() {
			@Override
			public void run() {
				String answerKBId = "https://www.tno.nl/example/relationProvider";
				String answerKIId = answerKBId + "/interaction/answerRelations";
				try {
					// register the AnswerKB
					HttpTester registerAnswerKb = new HttpTester(new URL(url + "/sc"), "POST",
							"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationProvider\", \"knowledgeBaseName\": \"RelationProvider\", \"knowledgeBaseDescription\": \"A KB that provides relations between people\", \"reasonerLevel\" : 2}",
							Map.of("Content-Type", "application/json", "Accept", "*/*"));
					registerAnswerKb.expectStatus(200);

					// register the AnswerKI
					HttpTester registerAnswerKi = new HttpTester(new URL(url + "/sc/ki"), "POST",
							"{\"knowledgeInteractionType\": \"AnswerKnowledgeInteraction\", \"knowledgeInteractionName\": \"answerRelations\", \"graphPattern\": \"?a <http://example.org/isRelatedTo> ?b .\"}",
							Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/relationProvider", "Content-Type",
									"application/json", "Accept", "*/*"));
					registerAnswerKi.expectStatus(200);

					KBReady.countDown();
					// get the handle for the answerKB to see if there are requests to be handled
					var test = new HttpTester(new URL(url.toString() + "/sc/handle"), "GET", null, Map
							.of("Knowledge-Base-Id", answerKBId, "Content-Type", "application/json", "Accept", "*/*"));
					test.expectStatus(200);

					// build the body to answer the request: add handle request ID and dummy data
					// bindingset
					JsonObjectBuilder builder = Json.createObjectBuilder();
					JsonReader jp = Json.createReader(new StringReader(test.getBody()));
					JsonObject jo = jp.readObject();
					int handleRequestId = jo.getInt("handleRequestId");
					builder.add("handleRequestId", handleRequestId);
					JsonReader jr = Json.createReader(new StringReader(
							"[{\"a\": \"<https://www.tno.nl/example/Barry>\",\"b\": \"<https://www.tno.nl/example/Jack>\"}]"));
					JsonArray bs = jr.readArray();
					builder.add("bindingSet", bs);
					JsonObject jo2 = builder.build();
					String body = jo2.toString();
					System.out.println("Handle an answer to a request with body: " + body);

					// fire the POST handle to execute the answer
					var test2 = new HttpTester(new URL(url.toString() + "/sc/handle"), "POST", body,
							Map.of("Knowledge-Base-Id", answerKBId, "Knowledge-Interaction-Id", answerKIId,
									"Content-Type", "application/json", "Accept", "*/*"));
					test2.expectStatus(200);

				} catch (MalformedURLException e) {
					fail();
				}
			}
		});
		answeringSc.start();

		KBReady.await();
		// register the AskKB
		HttpTester registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationAsker\", \"knowledgeBaseName\": \"RelationAsker\", \"knowledgeBaseDescription\": \"A KB that asks for relations between people\", \"reasonerLevel\" : 2}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);


		// register the AskKI
		HttpTester registerKiWithoutGapsEnabled = new HttpTester(new URL(url + "/sc/ki"), "POST",
				"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"knowledgeInteractionName\": \"askRelations\", \"graphPattern\": \"?a <http://example.org/isRelatedTo> ?b . ?a <http://example.org/isFatherOf> ?c .\", \"knowledgeGapsEnabled\": true}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/relationAsker", "Content-Type",
						"application/json", "Accept", "*/*"));
		registerKiWithoutGapsEnabled.expectStatus(200);

		// fire the ask KI
		HttpTester askKiWithoutGapsEnabled = new HttpTester(new URL(url + "/sc/ask"), "POST",
				"{\"recipientSelector\": {\"knowledgeBases\": []}, \"bindingSet\": []}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/relationAsker", "Knowledge-Interaction-Id",
						"https://www.tno.nl/example/relationAsker/interaction/askRelations", "Content-Type",
						"application/json", "Accept", "*/*"));
		var result = askKiWithoutGapsEnabled.getBody();
		System.out.println("Result is:" + result);
		assertTrue(result.contains("\"knowledgeGaps\":[[\"?a <http://example.org/isFatherOf> ?c\"]]"));

	}

	@AfterAll
	public void cleanUp() throws MalformedURLException {

		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}

}
