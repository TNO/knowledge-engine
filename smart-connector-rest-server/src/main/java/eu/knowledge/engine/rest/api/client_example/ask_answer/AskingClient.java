package eu.knowledge.engine.rest.api.client_example.ask_answer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.client_example.KnowledgeEngineRestApiClient;

/**
 * This class provides a client of the REST API with a proactive knowledge
 * interaction (an ASK). To demonstrate, it should be used with
 * {@link AnsweringClient}. First launch the {@link AnsweringClient}, and then
 * this {@link AskingClient}.
 */
public class AskingClient {
	private static final Logger LOG = LoggerFactory.getLogger(AskingClient.class);

	public static void main(String[] args) throws IOException, InterruptedException {
		var client = new KnowledgeEngineRestApiClient(
			"http://localhost:8280/rest",
			"https://www.example.org/asking-kb-" + UUID.randomUUID().toString(),
			"Wants to know about relations.",
			"This knowledge base wants to know all about which things are related to which other things!"
		);

		// Post an ASK KI.
		String ki = client.registerAsk(
			"?a <https://www.example.org/isRelatedTo> ?b."
		);
		LOG.info("Made new KI with ID {}", ki);
		
		// ASK something from the proactive side.
		var result = client.postAsk(ki);
		LOG.info("Got ASK result: {}", result);

		// ASK something else from the proactive side, with partial bindings
		var moreBindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>"));
		LOG.info("Sending ASK: {}", moreBindings);
		var moreResults = client.postAsk(ki, moreBindings);
		LOG.info("Got ASK result: {}", moreResults);

		LOG.info(
			"Aha, so {} is related to {}!",
			moreResults.getBindingSet().get(0).get("a"),
			moreResults.getBindingSet().get(0).get("b")
		);

		client.close();
	}
}
