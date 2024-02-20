package eu.knowledge.engine.rest.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
public class TestRegisterKnowledgeInteraction {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	@Disabled
	public void testRegisterKi() throws IOException {
		URL url = new URL("http://localhost:" + PORT + "/rest");

		HttpTester registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"http://example.com/kb\", \"knowledgeBaseName\": \"KB\", \"knowledgeBaseDescription\": \"KB\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);

		HttpTester registerKiWithName = new HttpTester(new URL(url + "/sc/ki"), "POST",
				"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"knowledgeInteractionName\": \"some-name\", \"graphPattern\": \"?a ?b ?c.\"}",
				Map.of("Knowledge-Base-Id", "http://example.com/kb", "Content-Type", "application/json", "Accept",
						"*/*"));
		registerKiWithName.expectStatus(200);

		HttpTester getKiWithName = new HttpTester(new URL(url + "/sc/ki"), "GET", null, Map.of("Knowledge-Base-Id",
				"http://example.com/kb", "Content-Type", "application/json", "Accept", "*/*"));
		var body = getKiWithName.getBody();
		assertTrue(body.contains("\"http://example.com/kb/interaction/some-name\""));
	}

	@AfterAll
	public void cleanUp() {
		System.out.println("Start clean up!");
		rsh.cleanUp();
		System.out.println("End clean up!");
	}
}
