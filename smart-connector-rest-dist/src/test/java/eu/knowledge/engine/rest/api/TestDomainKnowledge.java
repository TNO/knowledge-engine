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
public class TestDomainKnowledge {
	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testDomainKnowledge() throws IOException {
		URL url = new URL("http://localhost:" + PORT + "/rest");

		HttpTester registerKb = new HttpTester(new URL(url + "/sc"), "POST",
				"{\"knowledgeBaseId\": \"http://example.com/kb\", \"knowledgeBaseName\": \"KB\", \"knowledgeBaseDescription\": \"KB\"}",
				Map.of("Content-Type", "application/json", "Accept", "*/*"));
		registerKb.expectStatus(200);

		String domainKnowledgePrefix = """
				    @prefix saref: <https://saref.etsi.org/core/> .
				""";
		String domainKnowledgeRules = """
					-> ( saref:Sensor rdfs:subClassOf saref:Device ) .
					(?x rdfs:subClassOf ?y), (?a rdf:type ?x) -> (?a rdf:type ?y) .
				""";

		// non-existing KB id.
		HttpTester addDomainKnowledge = new HttpTester(new URL(url + "/sc/knowledge"), "POST",
				domainKnowledgePrefix + domainKnowledgeRules,
				Map.of("Knowledge-Base-Id", "http://example.com/kb123", "Content-Type", "text/plain", "Accept", "*/*"));
		addDomainKnowledge.expectStatus(404);

		// happy flow
		addDomainKnowledge = new HttpTester(new URL(url + "/sc/knowledge"), "POST",
				domainKnowledgePrefix + domainKnowledgeRules,
				Map.of("Knowledge-Base-Id", "http://example.com/kb", "Content-Type", "text/plain", "Accept", "*/*"));
		addDomainKnowledge.expectStatus(200);

		// domain knowledge with syntax error (missing prefix)
		addDomainKnowledge = new HttpTester(new URL(url + "/sc/knowledge"), "POST", domainKnowledgeRules,
				Map.of("Knowledge-Base-Id", "http://example.com/kb", "Content-Type", "text/plain", "Accept", "*/*"));
		addDomainKnowledge.expectStatus(400);
		System.out.println("Body: " + addDomainKnowledge.getBody());
		
		// remove all domain knowledge
		addDomainKnowledge = new HttpTester(new URL(url + "/sc/knowledge"), "POST", null,
				Map.of("Knowledge-Base-Id", "http://example.com/kb", "Content-Type", "text/plain", "Accept", "*/*"));
		addDomainKnowledge.expectStatus(200);
		System.out.println("Body: " + addDomainKnowledge.getBody());

	}

	@AfterAll
	public void cleanUp() {
		TestUtil.unregisterAllKBs("http://localhost:" + PORT + "/rest");
		rsh.cleanUp();
	}
}
