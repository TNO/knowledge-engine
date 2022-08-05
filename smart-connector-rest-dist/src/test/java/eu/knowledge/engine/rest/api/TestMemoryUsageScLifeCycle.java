package eu.knowledge.engine.rest.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.RestServerHelper;
import eu.knowledge.engine.test_utils.AsyncTester;
import eu.knowledge.engine.test_utils.HttpTester;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMemoryUsageScLifeCycle {

	private static final Logger LOG = LoggerFactory.getLogger(TestMemoryUsageScLifeCycle.class);

	private final RestServerHelper rsh = new RestServerHelper();
	private static int PORT = 8280;

	@BeforeAll
	public void setUpServer() {
		rsh.start(PORT);
	}

	@Test
	public void testAddingAndRemovingKBs() throws IOException {
		URL url = new URL("http://localhost:" + PORT + "/rest/sc");

		String[] kbIds = new String[] { "http://example.com/kb1", "http://example.com/kb2", "http://example.com/kb3" };
		String[] kbNames = new String[] { "KB1", "KB2", "KB3" };

		LOG.info("Starting 3 KBs that continuously add/remove their smart connector.");

		// create 3 kbs
		AsyncTester[] kbTester = new AsyncTester[3];
		for (int i = 0; i < kbIds.length; i++) {
			String id = kbIds[i];
			String name = kbNames[i];

			kbTester[i] = new AsyncTester(new Runnable() {

				@Override
				public void run() {

					int delayedStart = 0;
					if (id.equalsIgnoreCase("http://example.com/kb1")) {
						delayedStart = 0;
					} else if (id.equalsIgnoreCase("http://example.com/kb2")) {
						delayedStart = 3;
					} else if (id.equalsIgnoreCase("http://example.com/kb3")) {
						delayedStart = 7;
					}
					try {
						Thread.sleep(delayedStart);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					while (true) {
						HttpTester httpCreateTest = new HttpTester(url, "POST",
								"{\"knowledgeBaseId\": \"" + id + "\", \"knowledgeBaseName\": \"" + name
										+ "\", \"knowledgeBaseDescription\": \"KB\"}",
								Map.of("Content-Type", "application/json", "Accept", "*/*"));
						httpCreateTest.expectStatus(200);
						LOG.info("Succesfully created KB {}", id);

						try {
							HttpTester httpCreateKITest = new HttpTester(new URL(url.toString() + "/ki"), "POST",
									"{\"knowledgeInteractionType\": \"AskKnowledgeInteraction\", \"graphPattern\": \"?a <http://example.org/isRelatedTo> ?b .\"}",
									Map.of("Content-Type", "application/json", "Accept", "*/*", "Knowledge-Base-Id",
											id));
							httpCreateKITest.expectStatus(200);
						} catch (MalformedURLException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						/*
						 * : AskKnowledgeInteraction graphPattern:
						 * "?a <http://example.org/isRelatedTo> ?b ."
						 * 
						 * 
						 */

						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						HttpTester httpDeleteTest = new HttpTester(url, "DELETE", null,
								Map.of("Content-Type", "application/json", "Accept", "*/*", "Knowledge-Base-Id", id));
						httpDeleteTest.expectStatus(200);

						LOG.info("Succesfully deleted KB {}", id);

						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			});
			kbTester[i].start();

		}

		LOG.info("All testers started.");

		for (AsyncTester tester : kbTester) {
			tester.joinAndRethrow();
		}

	}

	@AfterAll
	public void cleanUp() {
		rsh.cleanUp();
	}
}
