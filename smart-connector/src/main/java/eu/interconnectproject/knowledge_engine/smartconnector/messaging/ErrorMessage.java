package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

public class ErrorMessage extends KnowledgeMessage {

	private final UUID replyToMessage;
	private final String errorMessage;

	protected ErrorMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToMessage, String errorMessage) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToMessage = replyToMessage;
		this.errorMessage = errorMessage;
	}

	public UUID getReplyToMessage() {
		return replyToMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
