package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

public abstract class KnowledgeMessage {

	protected UUID messageId;
	protected URI fromKnowledgeBase;
	protected URI fromKnowledgeInteraction;
	protected URI toKnowledgeBase;
	protected URI toKnowledgeInteraction;

	private final String failedMessage;

	protected KnowledgeMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction) {
		this(UUID.randomUUID(), fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
	}

	protected KnowledgeMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, String aFailedMessage) {
		this(UUID.randomUUID(), fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, aFailedMessage);
	}

	protected KnowledgeMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction) {
		this(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction, null);
	}

	protected KnowledgeMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, String aFailedMessage) {
		this.messageId = messageId;
		this.fromKnowledgeBase = fromKnowledgeBase;
		this.fromKnowledgeInteraction = fromKnowledgeInteraction;
		this.toKnowledgeBase = toKnowledgeBase;
		this.toKnowledgeInteraction = toKnowledgeInteraction;

		// validate
		if (fromKnowledgeBase == null) {
			throw new IllegalArgumentException("fromKnowledgeBase cannot be null");
		}
		if (fromKnowledgeInteraction == null) {
			throw new IllegalArgumentException("fromKnowledgeInteraction cannot be null");
		}
		if (toKnowledgeBase == null) {
			throw new IllegalArgumentException("toKnowledgeBase cannot be null");
		}
		if (toKnowledgeInteraction == null) {
			throw new IllegalArgumentException("toKnowledgeInteraction cannot be null");
		}

		this.failedMessage = aFailedMessage;
	}

	public UUID getMessageId() {
		return messageId;
	}

	public URI getFromKnowledgeBase() {
		return fromKnowledgeBase;
	}

	public URI getFromKnowledgeInteraction() {
		return fromKnowledgeInteraction;
	}

	public URI getToKnowledgeBase() {
		return toKnowledgeBase;
	}

	public URI getToKnowledgeInteraction() {
		return toKnowledgeInteraction;
	}

	public String getFailedMessage() {
		return this.failedMessage;
	}

}
