package eu.knowledge.engine.smartconnector.impl;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.smartconnector.api.PostResult;

public interface KnowledgeBaseStoreListener {

	void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki);

	void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki);

	/**
	 * The Knowledge Base has requested the smart connector to stop.
	 * 
	 * @return a future that will complete once all other knowledge bases have been
	 *         notified of our termination.
	 */
	CompletableFuture<PostResult> smartConnectorStopping();

}
