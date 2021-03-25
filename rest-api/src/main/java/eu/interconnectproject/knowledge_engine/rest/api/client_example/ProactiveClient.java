package eu.interconnectproject.knowledge_engine.rest.api.client_example;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a client of the REST API with a proactive knowledge
 * interaction (a POST). To demonstrate, it should be used with
 * {@link ReactiveClient}. First launch the {@link ReactiveClient}, and then
 * this {@link ProactiveClient}.
 */
public class ProactiveClient {
	private static final Logger LOG = LoggerFactory.getLogger(ProactiveClient.class);
	public static void main(String[] args) throws IOException, InterruptedException {
		var client = new Client("http://localhost:8080/rest");

		// Post a new SC with a POST KI.
		client.postSc("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/a-kb", "A knowledge base", "A very descriptive piece of text.");
		String ki1 = client.postKiPost("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/a-kb",
			"?a ?b ?c.",
			"?d ?e ?f.",
			Arrays.asList("https://www.tno.nl/energy/ontology/interconnect#InformPurpose"),
			Arrays.asList("https://www.tno.nl/energy/ontology/interconnect#InformPurpose")
		);
		LOG.info("Made new KI with ID {}", ki1);
		
		// Wait a bit... TODO: Can we get rid of this?
		Thread.sleep(1000);
		
		// Post something from the proactive side.
		var bindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>", "c", "<c>"));
		LOG.info("Sending POST: {}", bindings);
		var result = client.postPost("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/a-kb", ki1, bindings);
		LOG.info("Got POST result: {}", result);

		// Post something else from the proactive side, to show that the reactive side continues to listen.
		var modeBindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>", "c", "<c>"));
		LOG.info("Sending POST: {}", modeBindings);
		var moreResults = client.postPost("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/a-kb", ki1, modeBindings);
		LOG.info("Got POST result: {}", moreResults);
	}
}
