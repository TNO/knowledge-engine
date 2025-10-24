package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import jakarta.json.JsonReader;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestAskAnswerIncludeMetaKIs {
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
		var otherSc = new AsyncTester(new Runnable() {
			@Override
			public void run() {
				try {
					// register the AnswerKB
					HttpTester registerOtherKb = new HttpTester(new URL(url + "/sc"), "POST",
							"{\"knowledgeBaseId\": \"https://www.tno.nl/example/otherKB\", \"knowledgeBaseName\": \"RelationProvider\", \"knowledgeBaseDescription\": \"A KB that provides relations between people\", \"reasonerLevel\" : 2}",
							Map.of("Content-Type", "application/json", "Accept", "*/*"));
					registerOtherKb.expectStatus(200);
					KBReady.countDown();
				} catch (MalformedURLException e) {
					fail();
				}
			}
		});
		otherSc.start();

		KBReady.await();

		// register the AnswerKB
		HttpTester registerAskKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/metadataAsker\", \"knowledgeBaseName\": \"RelationProvider\", \"knowledgeBaseDescription\": \"A KB that provides relations between people\", \"reasonerLevel\" : 2}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerAskKb.expectStatus(200);

		// register the AskKI with IncludeMetaKIs enabled
		HttpTester registerAskKiWithIncludeMetaKIs = new HttpTester(new URL(url + "/sc/ki"), "POST",
				"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"knowledgeInteractionName\": \"askMetadataWithIncludeMetaKIs\", \"includeMetaKIs\": \"true\", \"graphPattern\": \"?kb <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> .\"}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/metadataAsker", "Content-Type",
						"application/json", "Accept", "*/*"));
		registerAskKiWithIncludeMetaKIs.expectStatus(200);

		// register the AskKI without IncludeMetaKIs (= disabled)
		HttpTester registerAskKiWithoutIncludeMetaKIs = new HttpTester(new URL(url + "/sc/ki"), "POST",
				"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"knowledgeInteractionName\": \"askMetadataWithoutIncludeMetaKIs\", \"graphPattern\": \"?kb <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> .\"}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/metadataAsker", "Content-Type",
						"application/json", "Accept", "*/*"));
		registerAskKiWithoutIncludeMetaKIs.expectStatus(200);

		// register the AskKI with IncludeMetaKIs disabled
		HttpTester registerAskKiWithIncludeMetaKIsDisabled = new HttpTester(new URL(url + "/sc/ki"), "POST",
				"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"knowledgeInteractionName\": \"askMetadataWithIncludeMetaKIsDisabled\", \"includeMetaKIs\": \"false\", \"graphPattern\": \"?kb <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> .\"}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/metadataAsker", "Content-Type",
						"application/json", "Accept", "*/*"));
		registerAskKiWithIncludeMetaKIsDisabled.expectStatus(200);

		// start asking

		HttpTester askAskKiWithIncludeMetaKIs = new HttpTester(new URL(url + "/sc/ask"), "POST",
				"{\"recipientSelector\": {\"knowledgeBases\": []}, \"bindingSet\": []}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/metadataAsker", "Knowledge-Interaction-Id",
						"https://www.tno.nl/example/metadataAsker/interaction/askMetadataWithIncludeMetaKIs",
						"Content-Type", "application/json", "Accept", "*/*"));
		System.out.println("Result is:" + askAskKiWithIncludeMetaKIs.getBody());

		JsonReader jsonReader = Json.createReader(new StringReader(askAskKiWithIncludeMetaKIs.getBody()));
		JsonObject jsonRoot = jsonReader.readObject();
		JsonArray jsonBindingSet = jsonRoot.getJsonArray("bindingSet");
		assertEquals(1, jsonBindingSet.size());

		HttpTester askAskKiWithoutIncludeMetaKIs = new HttpTester(new URL(url + "/sc/ask"), "POST",
				"{\"recipientSelector\": {\"knowledgeBases\": []}, \"bindingSet\": []}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/metadataAsker", "Knowledge-Interaction-Id",
						"https://www.tno.nl/example/metadataAsker/interaction/askMetadataWithoutIncludeMetaKIs",
						"Content-Type", "application/json", "Accept", "*/*"));
		System.out.println("Result is:" + askAskKiWithoutIncludeMetaKIs.getBody());
		jsonReader = Json.createReader(new StringReader(askAskKiWithoutIncludeMetaKIs.getBody()));
		jsonRoot = jsonReader.readObject();
		jsonBindingSet = jsonRoot.getJsonArray("bindingSet");
		assertEquals(0, jsonBindingSet.size());

		HttpTester askAskKiWithIncludeMetaKIsDisabled = new HttpTester(new URL(url + "/sc/ask"), "POST",
				"{\"recipientSelector\": {\"knowledgeBases\": []}, \"bindingSet\": []}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/metadataAsker", "Knowledge-Interaction-Id",
						"https://www.tno.nl/example/metadataAsker/interaction/askMetadataWithIncludeMetaKIsDisabled",
						"Content-Type", "application/json", "Accept", "*/*"));
		System.out.println("Result is:" + askAskKiWithIncludeMetaKIsDisabled.getBody());
		jsonReader = Json.createReader(new StringReader(askAskKiWithIncludeMetaKIsDisabled.getBody()));
		jsonRoot = jsonReader.readObject();
		jsonBindingSet = jsonRoot.getJsonArray("bindingSet");
		assertEquals(0, jsonBindingSet.size());

	}

	@AfterAll
	public void cleanUp() throws MalformedURLException {

		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}

}
