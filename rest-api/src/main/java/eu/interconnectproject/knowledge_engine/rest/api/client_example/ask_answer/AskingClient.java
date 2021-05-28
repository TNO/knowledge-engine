package eu.interconnectproject.knowledge_engine.rest.api.client_example.ask_answer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.client_example.RestApiClient;

/**
 * This class provides a client of the REST API with a proactive knowledge
 * interaction (an ASK). To demonstrate, it should be used with
 * {@link AnsweringClient}. First launch the {@link AnsweringClient}, and then
 * this {@link AskingClient}.
 */
public class AskingClient {
	private static final Logger LOG = LoggerFactory.getLogger(AskingClient.class);

	public static void main(String[] args) throws IOException, InterruptedException {
		var client = new RestApiClient("http://localhost:8280/rest", "https://www.example.org/asking-kb-" + UUID.randomUUID().toString(), "A knowledge base", "A very descriptive piece of text.");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOG.info("Cleaning up after myself!");
			client.cleanUp();
		}));

		// Post an ASK KI.
		String ki1 = client.registerAsk(
			"?a ?b ?c."
		);
		LOG.info("Made new KI with ID {}", ki1);
		
		// Wait a bit... TODO: Can we get rid of this?
		Thread.sleep(1000);
		
		// ASK something from the proactive side.
		var result = client.postAsk(ki1);
		LOG.info("Got ASK result: {}", result);

		// ASK something else from the proactive side, with partial bindings
		var moreBindings = Arrays.asList(Map.of("a", "<a>", "b", "<b>"));
		LOG.info("Sending ASK: {}", moreBindings);
		var moreResults = client.postAsk(ki1, moreBindings);
		LOG.info("Got ASK result: {}", moreResults);

		try {
			var incorrectBindings = Arrays.asList(Map.of("x", "<x>"));
			LOG.info("Sending ASK: {}", incorrectBindings);
			client.postAsk(ki1, incorrectBindings);
		} catch (RuntimeException e) {
			LOG.info("Encountered an expected RuntimeException:", e);
			LOG.info("Everything worked as expected, the exception above was a test.");
		}
	}
}
