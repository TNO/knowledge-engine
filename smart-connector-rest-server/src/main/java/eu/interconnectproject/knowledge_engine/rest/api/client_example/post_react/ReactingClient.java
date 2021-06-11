package eu.interconnectproject.knowledge_engine.rest.api.client_example.post_react;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.client_example.KnowledgeHandler;
import eu.interconnectproject.knowledge_engine.rest.api.client_example.KnowledgeEngineRestApiClient;
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

	public static void main(String[] args) throws InterruptedException {
		var client = new KnowledgeEngineRestApiClient(
			"http://localhost:8280/rest",
			"https://www.example.org/reacting-kb-" + UUID.randomUUID().toString(),
			"Celsius to Kelvin converter",
			"This knowledge base can convert Celsius to Kelvin!"
		);

		// Post a REACT KI.
		String ki = client.registerReact(
			"?a <https://www.example.org/measuredTemperatureInCelsius> ?b.",
			"?c <https://www.example.org/measuredTemperatureInKelvin> ?d.",
			new KnowledgeHandler() {
				@Override
				public HandleResponse handle(HandleRequest hr) {
					// Create new bindings with 
					var bindings = Arrays.asList(Map.of(
						"c", hr.getBindingSet().get(0).get("a"),
						"d", celsiusToKelvin(Double.parseDouble(hr.getBindingSet().get(0).get("b")))
					));
					return new HandleResponse().bindingSet(bindings).handleRequestId(hr.getHandleRequestId());
				}
			}
		);
		LOG.info("Made new KI with ID {}", ki);

		// Before starting the long poll loop, we need to make sure that we clean up
		// on shutdown.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			client.close();
		}));

		// Start long polling. This will trigger the handler when a POST is incoming.
		client.startLongPoll();
	}

	private static String celsiusToKelvin(double celsius) {
		return String.format("%f", celsius + 273.15);
	}
}
