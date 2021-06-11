package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.DistributedMessageDispatcher;

/**
 * This is a central static class for the Knowledge Engine runtime. This class
 * starts and provides access to runtime services for the Knowledge Engine for
 * which only one instance exists per JVM, and which can be used by any
 * component of the Knowledge Engine.
 */
public class KeRuntime {

	private static final Logger LOG = LoggerFactory.getLogger(KeRuntime.class);

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static KnowledgeDirectoryProxy knowledgeDirectory = new KnowledgeDirectoryImpl();
	private static ScheduledExecutorService executorService;
	private static DistributedMessageDispatcher messageDispatcher = null;

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

	public static DistributedMessageDispatcher getMessageDispatcher() {
		if (messageDispatcher == null) {
			try {
				messageDispatcher = new DistributedMessageDispatcher(getConfigProperty("HOSTNAME", "localhost"),
						Integer.parseInt(getConfigProperty("PORT", "8081")),
						getConfigProperty("KD_HOSTNAME", "localhost"),
						Integer.parseInt(getConfigProperty("KD_PORT", "8080")));
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

}
