package eu.knowledge.engine.rest.api.client_example.ask_answer;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.model.HandleRequest;
import eu.knowledge.engine.rest.model.HandleResponse;
import eu.knowledge.engine.rest.model.KnowledgeInteractionId;
import eu.knowledge.engine.rest.api.client_example.KnowledgeEngineRestApiClient;
import eu.knowledge.engine.rest.api.client_example.KnowledgeHandler;

/**
 * This class provides a client of the REST API with a reactive knowledge
 * interaction (an ANSWER). To demonstrate, it should be used with
 * {@link AskingClient}. First launch this {@link AnsweringClient}, and then
 * the {@link AskingClient}.
 */
public class AnsweringClient {
	private static final Logger LOG = LoggerFactory.getLogger(AnsweringClient.class);

	public static void main(String[] args) throws InterruptedException {
		var client = new KnowledgeEngineRestApiClient(
			"http://localhost:8280/rest",
			"https://www.example.org/answering-kb-" + UUID.randomUUID().toString(),
			"Relation knowledge base",
			"This knowledge base knows about things that are related to one another."
		);

		// Post an ANSWER KI.
		KnowledgeInteractionId ki = client.registerAnswer(
			"?a <https://www.example.org/isRelatedTo> ?b.",
			new KnowledgeHandler() {
				@Override
				public HandleResponse handle(HandleRequest handleRequest) {
					var bindings = Arrays.asList(Map.of(
						"a", "<https://www.example.org/Math>",
						"b", "<https://www.example.org/Science>"
					));
					return new HandleResponse().bindingSet(bindings).handleRequestId(handleRequest.getHandleRequestId());
				}
			}
		);
		LOG.info("Made new KI with ID {}", ki);

		// Before starting the long poll loop, we need to make sure that we clean up
		// on shutdown.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			client.close();
		}));

		// Start long polling. This will trigger the handler when an ASK is incoming.
		client.startLongPoll();
	}
}
