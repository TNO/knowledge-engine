package interconnect.ke.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import interconnect.ke.api.runtime.KnowledgeDirectory;
import interconnect.ke.api.runtime.LocalSmartConnectorRegistry;

public class KeRuntime {

	private static LocalSmartConnectorRegistry localSmartConnectorRegistry = new LocalSmartConnectorRegistryImpl();
	private static KnowledgeDirectory knowledgeDirectory = new KnowledgeDirectoryImpl();
	private static ExecutorService executorService = Executors.newFixedThreadPool(4);

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
