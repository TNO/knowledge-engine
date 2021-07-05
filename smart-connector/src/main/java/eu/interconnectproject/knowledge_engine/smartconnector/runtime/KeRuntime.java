package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.MessageDispatcher;

/**
 * This is a central static class for the Knowledge Engine runtime. This class
 * starts and provides access to runtime services for the Knowledge Engine for
 * which only one instance exists per JVM, and which can be used by any
 * component of the Knowledge Engine.
 */
public class KeRuntime {

	private static final String CONF_KEY_MY_HOSTNAME = "HOSTNAME";
	private static final String CONF_KEY_MY_PORT = "PORT";
	private static final String CONF_KEY_KD_HOSTNAME = "KD_HOSTNAME";
	private static final String CONF_KEY_KD_PORT = "KD_PORT";

	private static final Logger LOG = LoggerFactory.getLogger(KeRuntime.class);

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static KnowledgeDirectoryProxy knowledgeDirectory = new KnowledgeDirectoryImpl();
	private static ScheduledExecutorService executorService;
	private static MessageDispatcher messageDispatcher = null;

	static {
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
		return knowledgeDirectory;
	}

	public static ScheduledExecutorService executorService() {
		return executorService;
	}

	public static MessageDispatcher getMessageDispatcher() {
		if (messageDispatcher == null) {
			try {
				if (!hasConfigProperty(CONF_KEY_KD_HOSTNAME) || !hasConfigProperty(CONF_KEY_KD_PORT)) {
					LOG.warn(
							"No configuration provided for Knowledge Directory, starting Knowledge Engine in local mode");
					messageDispatcher = new MessageDispatcher();
				} else {
					messageDispatcher = new MessageDispatcher(
							getConfigProperty(CONF_KEY_MY_HOSTNAME, "localhost"),
							Integer.parseInt(getConfigProperty(CONF_KEY_MY_PORT, "8081")),
							getConfigProperty(CONF_KEY_KD_HOSTNAME, "localhost"),
							Integer.parseInt(getConfigProperty(CONF_KEY_KD_PORT, "8080")));
				}
			} catch (NumberFormatException e) {
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
