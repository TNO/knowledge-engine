package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.knowledge.engine.reasoner.api.BindingSet;

public class AnswerMessage extends KnowledgeMessage {

	private final UUID replyToAskMessage;
	/**
	 * Bindings for this AnswerMessage. Variable names of the
	 * AnswerKnowledgeInteraction are used. (Proactive side does the translations)
	 */
	private final BindingSet bindingSet;

	public AnswerMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings, String aFailedMessage) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, aFailedMessage);
		this.replyToAskMessage = replyToAskMessage;
		this.bindingSet = bindings;
	}

	public AnswerMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings) {
		this(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, replyToAskMessage, bindings, null);
	}

	public AnswerMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings, String aFailedMessage) {
		super(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, aFailedMessage);
		this.replyToAskMessage = replyToAskMessage;
		this.bindingSet = bindings;
	}

	public AnswerMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings) {
		this(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, replyToAskMessage, bindings, null);
	}

	public AnswerMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, String aFailedMessage) {
		this(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, replyToAskMessage, new BindingSet(), aFailedMessage);
	}

	public UUID getReplyToAskMessage() {
		return replyToAskMessage;
	}

	public BindingSet getBindings() {
		return bindingSet;
	}

	@Override
	public String toString() {
		return "AnswerMessage [getFromKnowledgeBase()=" + getFromKnowledgeBase() + ", getFromKnowledgeInteraction()="
				+ getFromKnowledgeInteraction() + ", getToKnowledgeBase()=" + getToKnowledgeBase()
				+ ", getToKnowledgeInteraction()=" + getToKnowledgeInteraction() + ", getMessageId()=" + getMessageId()
				+ ", getFailedMessage()=" + getFailedMessage() + ", getReplyToAskMessage()=" + getReplyToAskMessage()
				+ ", getBindings()=" + getBindings() + "]";
	}

}
