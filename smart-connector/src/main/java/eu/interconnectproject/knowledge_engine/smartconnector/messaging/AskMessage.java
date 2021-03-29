package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class AskMessage extends KnowledgeMessage {

	/**
	 * Bindings for this AskMessage. Variable names of the
	 * AnswerKnowledgeInteraction are used.
	 * (Proactive side does the translations)
	 */
	private BindingSet bindings;

	public AskMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.bindings = bindings;
	}

	public BindingSet getBindings() {
		return bindings;
	}

	@Override
	public String toString() {
		return "AskMessage [getToKnowledgeBase()=" + getToKnowledgeBase() + ", getToKnowledgeInteraction()="
				+ getToKnowledgeInteraction() + ", getFromKnowledgeBase()=" + getFromKnowledgeBase()
				+ ", getFromKnowledgeInteraction()=" + getFromKnowledgeInteraction() + ", getMessageId()="
				+ getMessageId() + ", getBindings()=" + getBindings() + "]";
	}

}
