package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.knowledge.engine.smartconnector.api.BindingSet;

public class ReactMessage extends KnowledgeMessage {

	private final UUID replyToPostMessage;

	/**
	 * Variable names of the React side are used. (Proactive side does the
	 * translations)
	 */
	private final BindingSet result;

	public ReactMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings, String aFailedMessage) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, aFailedMessage);
		this.replyToPostMessage = replyToPostMessage;
		this.result = bindings;
	}

	public ReactMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings) {
		this(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, replyToPostMessage, bindings, null);
	}

	public ReactMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings, String aFailedMessage) {
		super(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, aFailedMessage);
		this.replyToPostMessage = replyToPostMessage;
		this.result = bindings;
	}

	public ReactMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings) {
		this(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, replyToPostMessage, bindings, null);
	}

	public ReactMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, String aFailedMessage) {
		this(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, replyToPostMessage, new BindingSet(), aFailedMessage);
	}

	public UUID getReplyToPostMessage() {
		return replyToPostMessage;
	}

	public BindingSet getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "ReactMessage [getFromKnowledgeBase()=" + getFromKnowledgeBase() + ", getFromKnowledgeInteraction()="
				+ getFromKnowledgeInteraction() + ", getToKnowledgeBase()=" + getToKnowledgeBase()
				+ ", getToKnowledgeInteraction()=" + getToKnowledgeInteraction() + ", getMessageId()=" + getMessageId()
				+ ", getReplyToPostMessage()=" + getReplyToPostMessage() + ", getResult()=" + getResult() + "]";
	}
}
