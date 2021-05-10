package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging.DistributedMessageDispatcher;

/**
 * This is a central static class for the Knowledge Engine runtime. This class
 * starts and provides access to runtime services for the Knowledge Engine for
 * which only one instance exists per JVM, and which can be used by any
 * component of the Knowledge Engine.
 */
public class KeRuntime {

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static KnowledgeDirectoryProxy knowledgeDirectory = new KnowledgeDirectoryImpl();
	private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
	private static DistributedMessageDispatcher messageDispatcher = null;

//	/**
//	 * The purpose of calling the constructor is to start the
//	 * JvmOnlyMessageDisptacher and make sure it registers at the
//	 * LocalSmartConnectorRegistry
//	 */
//	private static JvmOnlyMessageDispatcher messageDispatcher = new JvmOnlyMessageDispatcher();

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
			// TODO make configurable
			messageDispatcher = new DistributedMessageDispatcher("localhost", 8081, "localhost", 8080);
			try {
				messageDispatcher.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return messageDispatcher;
	}

}
