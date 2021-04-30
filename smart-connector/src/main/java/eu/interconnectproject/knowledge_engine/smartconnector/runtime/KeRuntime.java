package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This is a central static class for the Knowledge Engine runtime. This class
 * starts and provides access to runtime services for the Knowledge Engine for
 * which only one instance exists per JVM, and which can be used by any
 * component of the Knowledge Engine.
 */
public class KeRuntime {

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static KnowledgeDirectory knowledgeDirectory = new KnowledgeDirectoryImpl();
	private static ExecutorService executorService;

	static {
		// we want to make sure that this threadpool does not keep the JVM alive. So we
		// set the daemon to true.
		executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				return t;
			}
		});
	}

	/**
	 * The purpose of calling the constructor is to start the
	 * JvmOnlyMessageDisptacher and make sure it registers at the
	 * LocalSmartConnectorRegistry
	 */
	private static JvmOnlyMessageDispatcher messageDispatcher = new JvmOnlyMessageDispatcher();

	public static LocalSmartConnectorRegistry localSmartConnectorRegistry() {
		return localSmartConnectorRegistry;
	}

	public static KnowledgeDirectory knowledgeDirectory() {
		return knowledgeDirectory;
	}

	public static ExecutorService executorService() {
		return executorService;
	}

}
