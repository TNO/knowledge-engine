package eu.knowledge.engine.rest.api.client_example.post_react;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.client_example.KnowledgeEngineRestApiClient;

/**
 * This class provides a client of the REST API with a proactive knowledge
 * interaction (a POST). To demonstrate, it should be used with
 * {@link ReactingClient}. First launch the {@link ReactingClient}, and then
 * this {@link PostingClient}.
 */
public class PostingClient {
	private static final Logger LOG = LoggerFactory.getLogger(PostingClient.class);

	public static void main(String[] args) throws IOException, InterruptedException {
		var client = new KnowledgeEngineRestApiClient(
			"http://localhost:8280/rest",
			"https://www.example.org/posting-kb-" + UUID.randomUUID().toString(),
			"Temperature sensor",
			"This temperature sensor publishes temperature measurements in Celsius, and expects them to be converted to Kelvin by someone else."
		);

		// Register a POST KI.
		String ki = client.registerPost(
			"?a <https://www.example.org/measuredTemperatureInCelsius> ?b.",
			"?c <https://www.example.org/measuredTemperatureInKelvin> ?d."
		);
		LOG.info("Made new KI with ID {}", ki);
		
		// TODO: https://gitlab.inesctec.pt/interconnect/knowledge-engine/-/issues/220
		Thread.sleep(1000);
		
		// Post something from the proactive side.
		var bindings = Arrays.asList(Map.of("a", "<https://www.example.org/some-sensor-1>", "b", "21.1"));
		LOG.info("Sending POST: {}", bindings);
		var result = client.postPost(ki, bindings);
		LOG.info("Got POST result: {}", result);

		// Post something else from the proactive side, to show that the reactive side continues to listen.
		var moreBindings = Arrays.asList(Map.of("a", "<https://www.example.org/some-sensor-1>", "b", "21.3"));
		LOG.info("Sending POST: {}", moreBindings);
		var moreResults = client.postPost(ki, moreBindings);
		LOG.info("Got POST result: {}", moreResults);

		client.close();
	}
}
