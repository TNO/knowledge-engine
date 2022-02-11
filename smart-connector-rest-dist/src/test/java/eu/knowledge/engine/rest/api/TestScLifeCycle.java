package eu.knowledge.engine.rest.api;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.HttpTester;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestScLifeCycle {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testInvalidJson() throws IOException {
		URL url = new URL("http://localhost:" + PORT + "/rest/sc");

		HttpTester httpTest = new HttpTester(url, "POST", "{\"bla\"{}", Map.of(
			"Content-Type", "application/json",
			"Accept", "*/*"
		));
		httpTest.expectStatus(400);
	}

	@Test
	public void testValidJson() throws IOException {
		URL url = new URL("http://localhost:" + PORT + "/rest/sc");

		HttpTester httpTest = new HttpTester(url, "POST", "{\"knowledgeBaseId\": \"http://example.com/kb\", \"knowledgeBaseName\": \"KB\", \"knowledgeBaseDescription\": \"KB\"}", Map.of(
			"Content-Type", "application/json",
			"Accept", "*/*"
		));
		httpTest.expectStatus(200);
	}

	@AfterAll
	public void cleanUp() {
		rsh.cleanUp();
	}
}
