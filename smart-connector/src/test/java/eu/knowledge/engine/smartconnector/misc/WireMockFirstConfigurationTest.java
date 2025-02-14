package eu.knowledge.engine.smartconnector.misc;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;
import eu.knowledge.engine.smartconnector.runtime.messaging.MessageDispatcher;
import eu.knowledge.engine.smartconnector.runtime.messaging.RemoteKerConnection;
import eu.knowledge.engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

@WireMockTest
public class WireMockFirstConfigurationTest {

	private static Logger LOG = LoggerFactory.getLogger(WireMockFirstConfigurationTest.class);

	@BeforeAll
	public static void before(WireMockRuntimeInfo wmRuntimeInfo) {

		// set all system property for various tests below.
		String host = wmRuntimeInfo.getHttpBaseUrl();
		System.setProperty(SmartConnectorConfig.CONF_KEY_KD_URL, host);
		System.setProperty(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_PORT, "1234");
		String value = "http://test${ke.runtime.hostname}:${ke.runtime.port}";
		System.setProperty(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL, value);

		LOG.info("Testing with exposed url: {}", value);
	}

	/**
	 * Does a (very limited) test of the http timeout configuration option. We are
	 * setting it to 1 second and if this works, the test should succeed within 2
	 * seconds. If setting the configuration property fails, the test would take 5
	 * seconds (= default value for http timeout configuration property).
	 * 
	 * @throws Exception
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	@Timeout(value = 3, unit = TimeUnit.SECONDS)
	public void testConfigHttpConnectTimeout() throws Exception {
		stubFor(post("/ker/").willReturn(status(201).withBody("{}")));
		System.setProperty(SmartConnectorConfig.CONF_KEY_KE_HTTP_TIMEOUT, "1");

		MessageDispatcher messageDispatcher = mock(MessageDispatcher.class);

		var ker = new RemoteKerConnection(messageDispatcher,
				new KnowledgeEngineRuntimeConnectionDetails().exposedUrl(URI.create("http://10.255.255.1/")));

		ker.start();
		assertFalse(ker.isAvailable());
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KE_HTTP_TIMEOUT);
	}

	@AfterAll
	public static void after() {
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KD_URL);
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_PORT);
		System.clearProperty(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL);
	}

}
