package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

public class ErrorMessage extends KnowledgeMessage {

	private final UUID replyToMessage;
	private final String errorMessage;

	public ErrorMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToMessage, String errorMessage) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToMessage = replyToMessage;
		this.errorMessage = errorMessage;
	}

	public ErrorMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToMessage, String errorMessage) {
		super(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToMessage = replyToMessage;
		this.errorMessage = errorMessage;
	}

	public UUID getReplyToMessage() {
		return replyToMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return "ErrorMessage [replyToMessage=" + replyToMessage + ", errorMessage=" + errorMessage + ", messageId="
				+ messageId + ", fromKnowledgeBase=" + fromKnowledgeBase + ", fromKnowledgeInteraction="
				+ fromKnowledgeInteraction + ", toKnowledgeBase=" + toKnowledgeBase + ", toKnowledgeInteraction="
				+ toKnowledgeInteraction + "]";
	}

}
