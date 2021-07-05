package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class ReactMessage extends KnowledgeMessage {

	private final UUID replyToPostMessage;

	/**
	 * Variable names of the React side are used. (Proactive side does the
	 * translations)
	 */
	private final BindingSet result;

	public ReactMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToPostMessage = replyToPostMessage;
		this.result = bindings;
	}

	public ReactMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings) {
		super(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToPostMessage = replyToPostMessage;
		this.result = bindings;
	}

	public UUID getReplyToPostMessage() {
		return replyToPostMessage;
	}

	public BindingSet getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "ReactMessage [getToKnowledgeBase()=" + getToKnowledgeBase() + ", getToKnowledgeInteraction()="
				+ getToKnowledgeInteraction() + ", getFromKnowledgeBase()=" + getFromKnowledgeBase()
				+ ", getFromKnowledgeInteraction()=" + getFromKnowledgeInteraction() + ", getMessageId()="
				+ getMessageId() + ", getReplyToPostMessage()=" + getReplyToPostMessage() + ", getResult()="
				+ getResult() + "]";
	}
}
