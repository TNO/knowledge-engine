package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.net.URI;
import java.util.Set;

public interface KnowledgeDirectoryProxy {

	Set<URI> getKnowledgeBaseIds();

	void addListener(KnowledgeDirectoryProxyListener listener);

	void removeListener(KnowledgeDirectoryProxyListener listener);

}
