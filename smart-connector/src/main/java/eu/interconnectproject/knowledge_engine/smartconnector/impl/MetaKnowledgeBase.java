package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import eu.interconnectproject.knowledge_engine.smartconnector.api.PostResult;

/**
 * This class is responsible for: - dereferencing knowledge base URIs into
 * {@link OtherKnowledgeBase} objects. - answering incoming meta ASK
 * interactions that ask for metaknowledge about this knowledge base. In short,
 * it is very similar to the {@InteractionProcessor} for the reactive
 * meta-knowledge messages, but for the proactive messages, it is also
 * responsible for parsing the knowledge base IDs into meta knowledge
 * interaction using a convention. This convention is also defined by the class
 * implementing this interface.
 */
public interface MetaKnowledgeBase {
	CompletableFuture<OtherKnowledgeBase> getOtherKnowledgeBase(URI knowledgeBaseId);

	void setOtherKnowledgeBaseStore(OtherKnowledgeBaseStore otherKnowledgeBaseStore);

	void setInteractionProcessor(InteractionProcessor interactionProcessor);

	CompletableFuture<PostResult> postNewKnowledgeBase();
}
