package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KnowledgeDirectory;

/**
 * An {@link OtherKnowledgeBaseStore} is responsible for keeping track of
 * metadata about knowledge bases in the knowledge network. In the MVP, it
 * should poll the network periodically for other {@link KnowledgeBase}s'
 * {@link KnowledgeInteraction}s and their {@link SmartConnectorImpl}s'
 * endpoints.
 *
 * It uses the {@link KnowledgeDirectory} to discover other smart connectors.
 */
public interface OtherKnowledgeBaseStore {

	/**
	 * Start the updating of the store.
	 */
	CompletableFuture<Void> start();

	/**
	 * Stop the updating of the store.
	 */
	void stop();

	/**
	 * @return The current list of {@link OtherKnowledgeBase}s.
	 */
	Set<OtherKnowledgeBase> getOtherKnowledgeBases();

}
