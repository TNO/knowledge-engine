package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KnowledgeDirectoryProxy;

/**
 * An {@link OtherKnowledgeBaseStore} is responsible for keeping track of
 * metadata about knowledge bases in the knowledge network. In the MVP, it
 * should poll the network periodically for other {@link KnowledgeBase}s'
 * {@link KnowledgeInteraction}s and their {@link SmartConnectorImpl}s'
 * endpoints.
 *
 * It uses the {@link KnowledgeDirectoryProxy} to discover other smart
 * connectors.
 */
public interface OtherKnowledgeBaseStore {

	/**
	 * Populate the store by sending ASK messages about metadata to all peers.
	 */
	CompletableFuture<Void> populate();

	/**
	 * @return The current list of {@link OtherKnowledgeBase}s.
	 */
	Set<OtherKnowledgeBase> getOtherKnowledgeBases();

	/**
	 * Update an already existing knowledge base with changed data.
	 *
	 * @param kb The knowledge base that has changed.
	 */
	void updateKnowledgeBase(OtherKnowledgeBase kb);

	/**
	 * Add a new knowledge base.
	 *
	 * @param kb The new knowledge base.
	 */
	void addKnowledgeBase(OtherKnowledgeBase kb);

	/**
	 * Remove a knowledge base
	 *
	 * @param kb The knowledge base that is to be removed.
	 */
	void removeKnowledgeBase(OtherKnowledgeBase kb);

	/**
	 * Shut down the OtherKnowledgeBaseStore
	 */
	void stop();
}
