package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.net.URI;

/**
 * A {@link ReactHandler} receives objects of this class when reacting to
 * interactions. It contains the incoming bindings, and also some other
 * metadata.
 */
public class ReactExchangeInfo {

	private final BindingSet argumentBindings;
	private final URI postingKnowledgeBaseId;
	private final URI postingKnowledgeInteractionId;

	public ReactExchangeInfo(
		BindingSet someArgumentBindings,
		URI aPostingKnowledgeBaseId, URI aPostingKnowledgeInteractionId
	) {
		this.postingKnowledgeBaseId = aPostingKnowledgeBaseId;
		this.postingKnowledgeInteractionId = aPostingKnowledgeInteractionId;
		this.argumentBindings = someArgumentBindings;
	}

	/**
	 * @return A {@link BindingSet}, containing the incoming bindings sent by the
	 * posting knowledge base (or the reasoner).
	 */
	public BindingSet getArgumentBindings() {
		return this.argumentBindings;
	}

	/**
	 * @return The ID of the knowledge base that initiated the interaction that
	 * caused your handler to be triggered.
	 */
	public URI getPostingKnowledgeBaseId() {
		return postingKnowledgeBaseId;
	}

	/**
	 * @return The ID of the knowledge interaction that initiated the interaction
	 * that caused your handler to be triggered.
	 */
	public URI getPostingKnowledgeInteractionId() {
		return postingKnowledgeInteractionId;
	}
}
