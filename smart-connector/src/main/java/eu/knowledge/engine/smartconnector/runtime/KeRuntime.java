package eu.knowledge.engine.smartconnector.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.runtime.messaging.MessageDispatcher;

/**
 * This is a central static class for the Knowledge Engine runtime. This class
 * starts and provides access to runtime services for the Knowledge Engine for
 * which only one instance exists per JVM, and which can be used by any
 * component of the Knowledge Engine.
 */
public class KeRuntime {

	private static final String CONF_KEY_MY_HOSTNAME = "KE_RUNTIME_HOSTNAME";
	private static final String CONF_KEY_MY_PORT = "KE_RUNTIME_PORT";
	private static final String CONF_KEY_KD_URL = "KD_URL";
	private static final String CONF_KEY_MY_EXPOSED_URL = "KE_RUNTIME_EXPOSED_URL";

	private static final String EXPOSED_URL_DEFAULT_PROTOCOL = "http";

	private static final Logger LOG = LoggerFactory.getLogger(KeRuntime.class);

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static ScheduledExecutorService executorService;
	private static MessageDispatcher messageDispatcher = null;

	static {
		if (hasConfigProperty(CONF_KEY_MY_EXPOSED_URL) && hasConfigProperty(CONF_KEY_MY_HOSTNAME)) {
			LOG.error("KE runtime must be configured with {} or {}, not both.", CONF_KEY_MY_EXPOSED_URL, CONF_KEY_MY_HOSTNAME);
			LOG.info("Using {} allows the use of a reverse proxy for TLS connections, which is recommended.", CONF_KEY_MY_EXPOSED_URL);
			System.exit(1);
		}

		// we want to make sure that this threadpool does not keep the JVM alive. So we
		// set the daemon to true.
		executorService = Executors.newScheduledThreadPool(12, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				return t;
			}
		});

		// Make sure the MessageDispatcher initializes
		getMessageDispatcher();
	}

	public static LocalSmartConnectorRegistry localSmartConnectorRegistry() {
		return localSmartConnectorRegistry;
	}

	public static KnowledgeDirectoryProxy knowledgeDirectory() {
		return messageDispatcher;
	}

	public static ScheduledExecutorService executorService() {
		return executorService;
	}

	public static MessageDispatcher getMessageDispatcher() {
		if (messageDispatcher == null) {
			try {
				if (!hasConfigProperty(CONF_KEY_KD_URL)) {
					LOG.warn(
							"No configuration provided for Knowledge Directory, starting Knowledge Engine in local mode");
					messageDispatcher = new MessageDispatcher();
				} else {
					var myHostname = getConfigProperty(CONF_KEY_MY_HOSTNAME, "localhost");
					var myPort = Integer.parseInt(getConfigProperty(CONF_KEY_MY_PORT, "8081"));
					
					URI myExposedUrl;
					if (hasConfigProperty(CONF_KEY_MY_EXPOSED_URL)) {
						myExposedUrl = new URI(getConfigProperty(CONF_KEY_MY_EXPOSED_URL, null));
					} else {
						// If no exposed URL config is given we build one based on the
						// configured host and port.
						myExposedUrl = new URI(EXPOSED_URL_DEFAULT_PROTOCOL + "://" + myHostname + ":" + myPort);
					}

					messageDispatcher = new MessageDispatcher(
						myPort,
						myExposedUrl,
						new URI(getConfigProperty(CONF_KEY_KD_URL, "http://localhost:8080"))
					);
				}
			} catch (NumberFormatException | URISyntaxException e) {
				LOG.error("Could not parse configuration properties, cannot start Knowledge Engine", e);
				System.exit(1);
			}
			try {
				messageDispatcher.start();
			} catch (Exception e) {
				LOG.error("Could not start HTTP server, cannot start Knowledge Engine", e);
				System.exit(1);
			}
		}
		return messageDispatcher;
	}

	public static String getConfigProperty(String key, String defaultValue) {
		// We might replace this with something a bit more fancy in the future...
		String value = System.getenv(key);
		if (value == null) {
			value = defaultValue;
			LOG.info("No value for the configuration parameter '" + key + "' was provided, using the default value '"
					+ defaultValue + "'");
		}
		return value;
	}

	public static boolean hasConfigProperty(String key) {
		return System.getenv(key) != null;
	}

}
