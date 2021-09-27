package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.knowledge.engine.smartconnector.api.BindingSet;

public class AnswerMessage extends KnowledgeMessage {

	private final UUID replyToAskMessage;
	/**
	 * Bindings for this AnswerMessage. Variable names of the
	 * AnswerKnowledgeInteraction are used. (Proactive side does the translations)
	 */
	private final BindingSet bindingSet;

	public AnswerMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToAskMessage = replyToAskMessage;
		this.bindingSet = bindings;
	}

	public AnswerMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings) {
		super(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToAskMessage = replyToAskMessage;
		this.bindingSet = bindings;
	}

	public UUID getReplyToAskMessage() {
		return replyToAskMessage;
	}

	public BindingSet getBindings() {
		return bindingSet;
	}

	@Override
	public String toString() {
		return "AnswerMessage [getToKnowledgeBase()=" + getToKnowledgeBase() + ", getToKnowledgeInteraction()="
				+ getToKnowledgeInteraction() + ", getFromKnowledgeBase()=" + getFromKnowledgeBase()
				+ ", getFromKnowledgeInteraction()=" + getFromKnowledgeInteraction() + ", getMessageId()="
				+ getMessageId() + ", getReplyToAskMessage()=" + getReplyToAskMessage() + ", getBindings()="
				+ getBindings() + "]";
	}

}
