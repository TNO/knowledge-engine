package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.net.URI;

public class AnswerExchangeInfo {

	private final BindingSet incomingBindings;
	private final URI askingKnowledgeBaseId;
	private final URI askingKnowledgeInteractionId;

	public AnswerExchangeInfo(
		BindingSet someIncomingBindings,
		URI anAskingKnowledgeBaseId, URI anAskingKnowledgeInteractionId
	) {
		this.askingKnowledgeBaseId = anAskingKnowledgeBaseId;
		this.askingKnowledgeInteractionId = anAskingKnowledgeInteractionId;
		this.incomingBindings = someIncomingBindings;
	}

	public BindingSet getIncomingBindings() {
		return this.incomingBindings;
	}

	public URI getAskingKnowledgeBaseId() {
		return askingKnowledgeBaseId;
	}

	public URI getAskingKnowledgeInteractionId() {
		return askingKnowledgeInteractionId;
	}
}
