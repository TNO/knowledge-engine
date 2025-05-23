package eu.knowledge.engine.rest.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.HttpTester;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestReasonerLevel {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testReasonerLevel() throws IOException, InterruptedException {

		URL url = new URL("http://localhost:" + PORT + "/rest");

		// register the AskKB with out of range reasoner level
		var registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationAsker\", \"knowledgeBaseName\": \"RelationAsker\", \"knowledgeBaseDescription\": \"A KB that asks for relations between people\", \"reasonerLevel\" : 0}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(400);

		// register the AskKB with out of range reasoner level
		registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationAsker\", \"knowledgeBaseName\": \"RelationAsker\", \"knowledgeBaseDescription\": \"A KB that asks for relations between people\", \"reasonerLevel\" : 6}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(400);

		// register the AskKB with correct reasoner level
		registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationAsker\", \"knowledgeBaseName\": \"RelationAsker\", \"knowledgeBaseDescription\": \"A KB that asks for relations between people\", \"reasonerLevel\" : 2}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);

		// register the AskKB without reasoner level
		var registerKb2 = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"https://www.tno.nl/example/relationAsker2\", \"knowledgeBaseName\": \"RelationAsker\", \"knowledgeBaseDescription\": \"A KB that asks for relations between people\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);
	}

	@AfterAll
	public void cleanUp() throws MalformedURLException {

		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}

}
