package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.net.URI;

/**
 * An {@link AnswerHandler} receives objects of this class when answering to
 * interactions. It contains the incoming bindings, and also some other
 * metadata.
 */
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

	/**
	 * @return A {@link BindingSet}, containing the incoming bindings sent by the
	 * asking knowledge base (or the reasoner).
	 */
	public BindingSet getIncomingBindings() {
		return this.incomingBindings;
	}

	/**
	 * @return The ID of the knowledge base that initiated the interaction that
	 * caused your handler to be triggered.
	 */
	public URI getAskingKnowledgeBaseId() {
		return askingKnowledgeBaseId;
	}

	/**
	 * @return The ID of the knowledge interaction that initiated the interaction
	 * that caused your handler to be triggered.
	 */
	public URI getAskingKnowledgeInteractionId() {
		return askingKnowledgeInteractionId;
	}
}
