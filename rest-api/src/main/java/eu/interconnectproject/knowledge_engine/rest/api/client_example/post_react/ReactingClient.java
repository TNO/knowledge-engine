package eu.interconnectproject.knowledge_engine.rest.api.client_example.post_react;

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
 * interaction (a REACT). To demonstrate, it should be used with
 * {@link PostingClient}. First launch this {@link ReactingClient}, and then
 * the {@link PostingClient}.
 */
public class ReactingClient {
	private static final Logger LOG = LoggerFactory.getLogger(ReactingClient.class);

	private static final String KB_ID = "https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/a-reacting-kb";

	public static void main(String[] args) throws InterruptedException {
		var client = new RestApiClient("http://localhost:8280/rest", KB_ID, "Another knowledge base", "Another very descriptive piece of text.");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOG.info("Cleaning up after myself!");
			client.cleanUp();
		}));

		// Post a REACT KI.
		String ki = client.postKiReact(KB_ID,
			"?a ?b ?c.",
			"?d ?e ?f.",
			new KnowledgeHandler() {
				@Override
				public HandleResponse handle(HandleRequest handleRequest) {
					LOG.info("I have to handle this request now: {}", handleRequest);
					var bindings = Arrays.asList(Map.of("d", "<d>", "e", "<e>", "f", "<f>"));
					return new HandleResponse().bindingSet(bindings).handleRequestId(handleRequest.getHandleRequestId());
				}
			}
		);
		LOG.info("Made new KI with ID {}", ki);

		// Start long polling. This will trigger the handler when a POST is incoming.
		client.startLongPoll(KB_ID);
	}
}
