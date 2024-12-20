package eu.knowledge.engine.admin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.MetaKB;
import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.util.MockedKnowledgeBase;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestApiRoutes {
	private Thread thread;
	private static final Logger LOG = LoggerFactory.getLogger(TestApiRoutes.class);

	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static MetaKB admin;
	private HttpClient httpClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	public void setUpServer() throws InterruptedException {
		admin = AdminUI.newInstance(false);
		httpClient = HttpClient.newBuilder().build();

		var r = new Runnable() {
			@Override
			public void run() {
				RestServer.main(new String[] {});
			}
		};
		this.thread = new Thread(r);
		this.thread.start();
		Thread.sleep(5000);
	}

	// todo: test with Knowledge directory -> needs second server (TKE runtime) in
	// new thread?
	// see KnowledgeDirectoryConnectionManagerTest.java.testSuccess for

	@Test
	public void testMethodNotAllowed() throws IOException {
		URL url = new URL("http://localhost:8283/admin/sc/all/false");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "*/*");
		conn.setDoOutput(true);

		OutputStream outStream = conn.getOutputStream();
		OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "UTF-8");
		outStreamWriter.write("{\"bla\"{}");
		outStreamWriter.flush();
		outStreamWriter.close();
		outStream.close();

		conn.connect();

		int responseCode = conn.getResponseCode();
		assertEquals(405, responseCode);
	}

	@Test
	public void testEmptyResult() {
		try {
			// todo: ask/poll if ready instead of waiting
			Thread.sleep(5000);
			URI uri = new URI("http://localhost:8283/admin/sc/all/true");
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			assertEquals(200, response.statusCode());
			eu.knowledge.engine.admin.model.SmartConnector[] result = objectMapper.readValue(response.body(),
					eu.knowledge.engine.admin.model.SmartConnector[].class);
			ArrayList<eu.knowledge.engine.admin.model.SmartConnector> list = new ArrayList<>();
			Collections.addAll(list, result);
			assertEquals(list, new ArrayList<>());
		} catch (IOException | InterruptedException | URISyntaxException e) {
			LOG.warn("Was not able to retrieve smart connectors", e);
			fail();
		}
	}

	@Test
	public void testSmartConnectorAllRoute() throws InterruptedException {
		startKbs();

		try {
			URI uri = new URI("http://localhost:8283/admin/sc/all/true");
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			eu.knowledge.engine.admin.model.SmartConnector[] result = objectMapper.readValue(response.body(),
					eu.knowledge.engine.admin.model.SmartConnector[].class);
			ArrayList<eu.knowledge.engine.admin.model.SmartConnector> list = new ArrayList<>();
			Collections.addAll(list, result);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertEquals(9, list.get(0).getKnowledgeInteractions().size());
			assertEquals(200, response.statusCode());
		} catch (IOException | InterruptedException | URISyntaxException e) {
			LOG.warn("Was not able to retrieve smart connectors", e);
			fail();
		}
		stopKbs();
	}

	@Test
	public void testConnections() throws InterruptedException {

		startKbs();
		try {
			URI uri = new URI("http://localhost:8283/admin/sc/all/false");
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			eu.knowledge.engine.admin.model.SmartConnector[] result = objectMapper.readValue(response.body(),
					eu.knowledge.engine.admin.model.SmartConnector[].class);
			ArrayList<eu.knowledge.engine.admin.model.SmartConnector> list = new ArrayList<>();
			Collections.addAll(list, result);

			System.out.println(list);

			assertNotNull(list);
			assertEquals(2, list.size());
			assertEquals(0, list.get(0).getKnowledgeInteractions().get(0).getConnections().size());
			assertEquals(1, list.get(1).getKnowledgeInteractions().get(0).getConnections().size());
			assertEquals(200, response.statusCode());

		} catch (IOException | InterruptedException | URISyntaxException e) {
			LOG.warn("Was not able to retrieve smart connectors", e);
			fail();
		}
		stopKbs();

	}

	@Test
	public void testSmartConnectorAllRouteWithoutMetaData() throws InterruptedException {
		startKbs();

		try {
			URI uri = new URI("http://localhost:8283/admin/sc/all/false");
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			eu.knowledge.engine.admin.model.SmartConnector[] result = objectMapper.readValue(response.body(),
					eu.knowledge.engine.admin.model.SmartConnector[].class);
			ArrayList<eu.knowledge.engine.admin.model.SmartConnector> list = new ArrayList<>();
			Collections.addAll(list, result);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertEquals(1, list.get(0).getKnowledgeInteractions().size());
			assertEquals(200, response.statusCode());
		} catch (IOException | InterruptedException | URISyntaxException e) {
			LOG.warn("Was not able to retrieve smart connectors", e);
			fail();
		}
		stopKbs();
	}

	@Test
	public void testSmartConnectorUpdate() throws InterruptedException {
		startKbs();

		try {
			URI uri = new URI("http://localhost:8283/admin/sc/all/false");
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			eu.knowledge.engine.admin.model.SmartConnector[] result = objectMapper.readValue(response.body(),
					eu.knowledge.engine.admin.model.SmartConnector[].class);
			ArrayList<eu.knowledge.engine.admin.model.SmartConnector> list = new ArrayList<>();
			Collections.addAll(list, result);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertEquals(1, list.get(0).getKnowledgeInteractions().size());
			assertEquals(200, response.statusCode());

			// now stop one KB and check if the admin UI correctly updates.
			stopKb(kb1);
			Thread.sleep(1000);

			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			result = objectMapper.readValue(response.body(), eu.knowledge.engine.admin.model.SmartConnector[].class);
			list = new ArrayList<>();
			Collections.addAll(list, result);
			assertEquals(1, list.size());
			assertEquals(200, response.statusCode());

		} catch (IOException | InterruptedException | URISyntaxException e) {
			LOG.warn("Was not able to retrieve smart connectors", e);
			fail();
		}
		stopKb(kb2);
	}

	@AfterAll
	public void cleanup() {
		LOG.info("Clean up: {}", TestApiRoutes.class.getSimpleName());

		if (admin != null) {
			admin.close();
		}
		thread.interrupt();
	}

	public void startKbs() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");
		kb1 = null;
		int wait = 2;
		final CountDownLatch kb2ReceivedData = new CountDownLatch(1);

		kb1 = new MockedKnowledgeBase("kb1") {
			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				LOG.info("smartConnector of {} ready.", this.name);
			}
		};

		GraphPattern gp1 = new GraphPattern(prefixes, "?a <https://www.tno.nl/example/b> ?c.");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kb1.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(anAnswerExchangeInfo.getIncomingBindings().isEmpty(),
					"Should not have bindings in this binding set.");

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("a", "<https://www.tno.nl/example/a>");
			binding.put("c", "<https://www.tno.nl/example/c>");
			bindingSet.add(binding);

			return bindingSet;
		});
		kb1.start();
		kb1.syncKIs();

		// todo: ask/poll if ready instead of waiting
		Thread.sleep(5000);
		kb2 = null;
		kb2 = new MockedKnowledgeBase("kb2") {
			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				LOG.info("smartConnector of {} ready.", this.name);

			}

		};

		GraphPattern gp2 = new GraphPattern(prefixes, "?x <https://www.tno.nl/example/b> ?y.");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);

		kb2.register(askKI);
		kb2.start();
		kb2.syncKIs();
		LOG.trace("After kb2 register");
		// todo: ask/poll if ready instead of waiting
		Thread.sleep(10000);
	}

	public void stopKbs() {
		stopKb(kb1);
		stopKb(kb2);
	}

	public void stopKb(MockedKnowledgeBase aKb) {
		if (aKb != null) {
			aKb.stop();
		}
	}
}
