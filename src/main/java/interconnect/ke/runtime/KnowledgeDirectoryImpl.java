package interconnect.ke.runtime;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.runtime.KnowledgeDirectory;

/**
 * This is a KnowledgeDirectory implementation that only provides information
 * for {@link SmartConnector}s inside this JVM! NEEDS TO BE REPLACED ONCE WE
 * SUPPORT DISTRIBUTED KNOWLEDGE ENGINES! TODO
 */
public class KnowledgeDirectoryImpl implements KnowledgeDirectory {

	private static KnowledgeDirectoryImpl instance;

	public static KnowledgeDirectoryImpl getInstance() {
		if (instance == null) {
			instance = new KnowledgeDirectoryImpl();
		}
		return instance;
	}

	private KnowledgeDirectoryImpl() {
	}

	@Override
	public Set<URI> getKnowledgeBaseIds() {
		return SmartConnectorRegistryImpl.getInstance().getSmartConnectors().stream()
				.map(sc -> sc.getEndpoint().getKnowledgeBaseId()).collect(Collectors.toSet());
	}

}
