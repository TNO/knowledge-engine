package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

/**
 * Listener for when the list of Knowledge Bases changes.
 *
 * New or deleted Smart Connectors announce their presence through their meta
 * knowledge interactions. However, in a distributed environment we cannot be
 * sure that we receive those messages. This Listener provides an alternative
 * method to get updated once the set of Smart Connectors changes.
 */
public interface KnowledgeDirectoryProxyListener {

	void knowledgeBaseIdSetChanged();

}
