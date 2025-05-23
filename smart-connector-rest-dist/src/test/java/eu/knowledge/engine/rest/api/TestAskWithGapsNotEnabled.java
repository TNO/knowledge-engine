package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.jena.atlas.logging.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.HttpTester;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestAskWithGapsNotEnabled {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testAskWithoutGaps() throws IOException {

		// In this test there will be only 1 KB with a single AskKI.
		// The test will execute the AskKI without knowledge gaps enabled.
		// As a result, the set of knowledge gaps should be null.

		URL url = new URL("http://localhost:" + PORT + "/rest");

		HttpTester registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationAsker\", \"knowledgeBaseName\": \"RelationAsker\", \"knowledgeBaseDescription\": \"A KB that asks for relations between people\", \"reasonerEnabled\" : true}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);

		HttpTester registerKiWithoutGapsEnabled = new HttpTester(new URL(url + "/sc/ki"), "POST",
				"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"knowledgeInteractionName\": \"askRelations\", \"graphPattern\": \"?a <http://example.org/isRelatedTo> ?b .\"}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/relationAsker", "Content-Type",
						"application/json", "Accept", "*/*"));
		registerKiWithoutGapsEnabled.expectStatus(200);

		HttpTester getKiWithoutGapsEnabled = new HttpTester(new URL(url + "/sc/ki"), "GET", null,
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/relationAsker", "Content-Type",
						"application/json", "Accept", "*/*"));
		var body = getKiWithoutGapsEnabled.getBody();
		assertTrue(body.contains("\"https://www.tno.nl/example/relationAsker/interaction/askRelations\""));

		HttpTester askKiWithoutGapsEnabled = new HttpTester(new URL(url + "/sc/ask"), "POST",
				"{\"recipientSelector\": {\"knowledgeBases\": []}, \"bindingSet\": []}",
				Map.of("Knowledge-Base-Id", "https://www.tno.nl/example/relationAsker", "Knowledge-Interaction-Id",
						"https://www.tno.nl/example/relationAsker/interaction/askRelations", "Content-Type",
						"application/json", "Accept", "*/*"));
		var result = askKiWithoutGapsEnabled.getBody();
		System.out.println("Result is:" + result);
		assertFalse(result.contains("\"knowledgeGaps\":"));
	}

	@AfterAll
	public void cleanUp() {
		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");

		rsh.cleanUp();
	}
}
