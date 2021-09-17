package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.net.URI;

public class ReactExchangeInfo {

	private final BindingSet argumentBindings;
	private final URI askingKnowledgeBaseId;
	private final URI askingKnowledgeInteractionId;

	public ReactExchangeInfo(
		BindingSet someArgumentBindings,
		URI aPostingKnowledgeBaseId, URI aPostingKnowledgeInteractionId
	) {
		this.askingKnowledgeBaseId = aPostingKnowledgeBaseId;
		this.askingKnowledgeInteractionId = aPostingKnowledgeInteractionId;
		this.argumentBindings = someArgumentBindings;
	}

	public BindingSet getArgumentBindings() {
		return this.argumentBindings;
	}

	public URI getPostingKnowledgeBaseId() {
		return askingKnowledgeBaseId;
	}

	public URI getPostingKnowledgeInteractionId() {
		return askingKnowledgeInteractionId;
	}
}
