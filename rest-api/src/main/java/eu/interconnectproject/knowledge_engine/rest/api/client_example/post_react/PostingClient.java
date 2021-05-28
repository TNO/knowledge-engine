package eu.interconnectproject.knowledge_engine.rest.api.client_example.post_react;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.client_example.RestApiClient;

/**
 * This class provides a client of the REST API with a proactive knowledge
 * interaction (a POST). To demonstrate, it should be used with
 * {@link ReactingClient}. First launch the {@link ReactingClient}, and then
 * this {@link PostingClient}.
 */
public class PostingClient {
	private static final Logger LOG = LoggerFactory.getLogger(PostingClient.class);

	public static void main(String[] args) throws IOException, InterruptedException {
		var client = new RestApiClient("http://localhost:8280/rest", "https://www.example.org/posting-kb-" + UUID.randomUUID().toString(), "A knowledge base", "A very descriptive piece of text.");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOG.info("Cleaning up after myself!");
			client.cleanUp();
		}));

		// Post a POST KI.
		String ki1 = client.registerPost(
			"?a ?b ?c.",
			"?d ?e ?f."
		);
		LOG.info("Made new KI with ID {}", ki1);
		
		// Wait a bit... TODO: Can we get rid of this?
		Thread.sleep(1000);
		
		// Post something from the proactive side.
		var bindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>", "c", "<c>"));
		LOG.info("Sending POST: {}", bindings);
		var result = client.postPost(ki1, bindings);
		LOG.info("Got POST result: {}", result);

		// Post something else from the proactive side, to show that the reactive side continues to listen.
		var moreBindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>", "c", "<c>"));
		LOG.info("Sending POST: {}", moreBindings);
		var moreResults = client.postPost(ki1, moreBindings);
		LOG.info("Got POST result: {}", moreResults);

		try {
			var incorrectBindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>"));
			LOG.info("Sending POST: {}", incorrectBindings);
			client.postPost(ki1, incorrectBindings);
		} catch (RuntimeException e) {
			LOG.info("Encountered an expected RuntimeException:", e);
			LOG.info("Everything worked as expected, the exception above was a test.");
		}
	}
}
