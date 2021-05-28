package eu.interconnectproject.knowledge_engine.rest.api.client_example.ask_answer;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.client_example.KnowledgeHandler;
import eu.interconnectproject.knowledge_engine.rest.api.client_example.RestApiClient;
import eu.interconnectproject.knowledge_engine.rest.model.HandleRequest;
import eu.interconnectproject.knowledge_engine.rest.model.HandleResponse;

/**
 * This class provides a client of the REST API with a reactive knowledge
 * interaction (an ANSWER). To demonstrate, it should be used with
 * {@link AskingClient}. First launch this {@link AnsweringClient}, and then
 * the {@link AskingClient}.
 */
public class AnsweringClient {
	private static final Logger LOG = LoggerFactory.getLogger(AnsweringClient.class);

	private static String KB_ID = "https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/an-answering-kb";

	public static void main(String[] args) throws InterruptedException {
		var client = new RestApiClient("http://localhost:8280/rest", KB_ID, "Another knowledge base", "Another very descriptive piece of text.");

		// Post an ANSWER KI.
		String ki = client.postKiAnswer(KB_ID,
			"?a ?b ?c.",
			new KnowledgeHandler() {
				@Override
				public HandleResponse handle(HandleRequest handleRequest) {
					LOG.info("I have to handle this request now: {}", handleRequest);
					var bindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>", "c", "<c>"));
					return new HandleResponse().bindingSet(bindings).handleRequestId(handleRequest.getHandleRequestId());
				}
			}
		);
		LOG.info("Made new KI with ID {}", ki);

		// Start long polling. This will trigger the handler when an ASK is incoming.
		client.startLongPoll(KB_ID);
	}
}
