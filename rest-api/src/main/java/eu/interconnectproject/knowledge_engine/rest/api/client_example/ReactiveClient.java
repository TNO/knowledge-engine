package eu.interconnectproject.knowledge_engine.rest.api.client_example;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.model.HandleRequest;
import eu.interconnectproject.knowledge_engine.rest.model.HandleResponse;

/**
 * This class provides a client of the REST API with a reactive knowledge
 * interaction (a REACT). To demonstrate, it should be used with
 * {@link ProactiveClient}. First launch this {@link ReactiveClient}, and then
 * the {@link ProactiveClient}.
 */
public class ReactiveClient {
	private static final Logger LOG = LoggerFactory.getLogger(ReactiveClient.class);

	public static void main(String[] args) throws InterruptedException {
		var client = new Client("http://localhost:8080/rest");
		
		// First remove all existing smart connectors (a bit nuclear, but it does
		// the trick)
		client.flushAll();

		// Post a SC with a REACT KI.
		client.postSc("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/another-kb", "Another knowledge base", "Another very descriptive piece of text.");
		String ki = client.postKiReact("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/another-kb",
			"?a ?b ?c.",
			"?d ?e ?f.",
			Arrays.asList("https://www.tno.nl/energy/ontology/interconnect#InformPurpose"),
			Arrays.asList("https://www.tno.nl/energy/ontology/interconnect#InformPurpose"),
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
		client.startLongPoll("https://www.interconnectproject.eu/knowledge-engine/knowledgebase/example/another-kb");
	}
}
