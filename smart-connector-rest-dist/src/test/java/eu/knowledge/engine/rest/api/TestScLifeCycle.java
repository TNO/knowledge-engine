package eu.knowledge.engine.rest.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.helpers.MessageFormatter;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.HttpTester;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestScLifeCycle {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	private String[] validKbIds = new String[] {
			//@formatter:off
			"https://asdlkasld.com",
			"mailto:hello",
			"https://hello",
			//@formatter:on
	};

	private String[] invalidKbIds = new String[] {
			//@formatter:off
			"",
			"https://",
			"strange characters and spaces | & ^",
			"/relative.nl"
			//@formatter:on
	};

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testInvalidJson() throws IOException, URISyntaxException {
		URL url = new URI("http://localhost:" + PORT + "/rest/sc").toURL();

		HttpTester httpTest = new HttpTester(url, "POST", "{\"bla\"{}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		httpTest.expectStatus(400);
	}

	@Test
	public void testValidJson() throws IOException, URISyntaxException {
		URL url = new URI("http://localhost:" + PORT + "/rest/sc").toURL();

		HttpTester httpTest = new HttpTester(url, "POST",
				"{\"knowledgeBaseId\": \"http://example.org/kb\", \"knowledgeBaseName\": \"KB\", \"knowledgeBaseDescription\": \"KB\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		httpTest.expectStatus(200);
	}

	@Test
	public void testKnowledgeBaseIdsValidity() throws MalformedURLException, URISyntaxException {
		URL url = new URI("http://localhost:" + PORT + "/rest/sc").toURL();

		String template = """
				{
					\"knowledgeBaseId\": \"{}\",
					\"knowledgeBaseName\": \"KB\",
					\"knowledgeBaseDescription\": \"KB\"
				}
				""";

		for (String kbId : this.validKbIds) {
			String jsonBody = MessageFormatter.format(template, kbId).getMessage();
			HttpTester httpTest = new HttpTester(url, "POST", jsonBody,
					Map.of("Content-Type", "application/json", "Accept", "*/*"));
			httpTest.expectStatus(200);
		}

		for (String kbId : this.invalidKbIds) {
			String jsonBody = MessageFormatter.format(template, kbId).getMessage();
			HttpTester httpTest = new HttpTester(url, "POST", jsonBody,
					Map.of("Content-Type", "application/json", "Accept", "*/*"));
			httpTest.expectStatus(400);
		}
	}

	@AfterAll
	public void cleanUp() {
		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}
}
