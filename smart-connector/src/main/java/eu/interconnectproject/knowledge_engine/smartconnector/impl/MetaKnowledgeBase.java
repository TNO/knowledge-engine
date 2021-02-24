package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;

/**
 * This class is responsible for:
 * - dereferencing knowledge base URIs into {@link OtherKnowledgeBase} objects. 
 * - answering incoming meta ASK interactions that ask for metaknowledge about
 *   this knowledge base.
 * In short, it is very similar to the {@InteractionProcessor} for the reactive
 * meta-knowledge messages, but for the proactive messages, it is also
 * responsible for parsing the knowledge base IDs into meta knowledge
 * interaction using a convention. This convention is also defined by the class
 * implementing this interface.
 */
public interface MetaKnowledgeBase {
	AnswerMessage processAskFromMessageRouter(AskMessage anAskMessage);
	ReactMessage processPostFromMessageRouter(PostMessage aPostMessage);

	CompletableFuture<OtherKnowledgeBase> getOtherKnowledgeBase(URI knowledgeBaseId);

	boolean isMetaKnowledgeInteraction(URI id);
	void setOtherKnowledgeBaseStore(OtherKnowledgeBaseStore otherKnowledgeBaseStore);
	
	/**
	 * Inform all knowledge bases in {@param otherKnowledgeBases} about the fact
	 * that this knowledge base just appeared.
	 * @param otherKnowledgeBases
	 */
	void postNewKnowledgeBase(Set<OtherKnowledgeBase> otherKnowledgeBases);

	/**
	 * Inform all knowledge bases in {@param otherKnowledgeBases} about the fact
	 * that this knowledge base just changed.
	 * @param otherKnowledgeBases
	 */
	void postChangedKnowledgeBase(Set<OtherKnowledgeBase> otherKnowledgeBases);

	/**
	 * Inform all knowledge bases in {@param otherKnowledgeBases} about the fact
	 * that this knowledge base just left.
	 * @param otherKnowledgeBases
	 */
	void postRemovedKnowledgeBase(Set<OtherKnowledgeBase> otherKnowledgeBases);
}
