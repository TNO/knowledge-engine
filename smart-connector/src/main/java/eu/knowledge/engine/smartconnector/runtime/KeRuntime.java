package eu.knowledge.engine.smartconnector.runtime;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;
import eu.knowledge.engine.smartconnector.runtime.messaging.MessageDispatcher;

/**
 * This is a central static class for the Knowledge Engine runtime. This class
 * starts and provides access to runtime services for the Knowledge Engine for
 * which only one instance exists per JVM, and which can be used by any
 * component of the Knowledge Engine.
 */
public class KeRuntime {
	private static final Logger LOG = LoggerFactory.getLogger(KeRuntime.class);

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static ScheduledExecutorService executorService;
	private static MessageDispatcher messageDispatcher = null;

	static {

		// although no guarantees can be made, let's try and shutdown gracefully and let
		// others know.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOG.warn("Graceful shutdown requested.");

			// Perform cleanup tasks here
			try {
				getMessageDispatcher().stop();
			} catch (Exception e) {
				LOG.error("No error should occur when stopping the message dispatcher.", e);
			}
		}));

		Config config = ConfigProvider.getConfig();
		ConfigValue exposedUrl = config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL);
		ConfigValue hostname = config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_HOSTNAME);

		// Using MicroProfile Config's source ordinal to determine if default
		// configuration got overridden?
		if (exposedUrl.getSourceOrdinal() > 100 && hostname.getSourceOrdinal() > 100) {
			LOG.error("KE runtime must be configured with {} or {}, not both.",
					SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL,
					SmartConnectorConfig.CONF_KEY_KE_RUNTIME_HOSTNAME);
			LOG.info("Using {} allows the use of a reverse proxy for TLS connections, which is recommended.",
					SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL);
			System.exit(1);
		}

		// execute some validation on the EXPOSED URL, because it can have severe
		// consequences
		if (exposedUrl.getSourceOrdinal() > 100) {
			String url = exposedUrl.getValue();
			if (url.endsWith("/")) {
				LOG.error(
						"The '{}' environment variable's value '{}' should be a valid URL without a slash ('/') as the last character.",
						SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL, url);
				System.exit(1);
			}
			try {
				new URL(url);

			} catch (MalformedURLException e) {
				LOG.error("The '{}' environment variable with value '{}' contains a malformed URL '{}'.",
						SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL, url, e.getMessage());
				System.exit(1);
			}
		}

		// we want to make sure that this threadpool does not keep the JVM alive. So we
		// set the daemon to true.
		executorService = Executors.newScheduledThreadPool(12, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setUncaughtExceptionHandler((aThread, e) -> {
					LOG.error("The following uncaught exception should not occur in thread {}.", aThread.getName(), e);
				});
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

		Config config = ConfigProvider.getConfig();

		if (messageDispatcher == null) {
			try {
				ConfigValue kdUrl = config.getConfigValue(SmartConnectorConfig.CONF_KEY_KD_URL);
				if (kdUrl.getSourceOrdinal() == 100) {
					LOG.warn(
							"No configuration provided for Knowledge Directory, starting Knowledge Engine in local mode");
					messageDispatcher = new MessageDispatcher();
				} else {
					ConfigValue port = config.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_PORT);
					var myPort = Integer.parseInt(port.getValue());

					ConfigValue exposedUrl = config
							.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_EXPOSED_URL);
					URI myExposedUrl = new URI(exposedUrl.getValue());

					ConfigValue useEdc = config
							.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_RUNTIME_USE_EDC);
					var myUseEdc = Boolean.parseBoolean(useEdc.getValue());

					messageDispatcher = new MessageDispatcher(myPort, myExposedUrl, new URI(kdUrl.getValue()), myUseEdc);
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
}
