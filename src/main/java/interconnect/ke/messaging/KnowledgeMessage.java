package interconnect.ke.messaging;

import java.net.URI;
import java.util.UUID;

public abstract class KnowledgeMessage {

	protected UUID messageId;
	protected URI fromKnowledgeBase;
	protected URI fromKnowledgeInteraction;
	protected URI toKnowledgeBase;
	protected URI toKnowledgeInteraction;

	protected KnowledgeMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction) {
		this.messageId = UUID.randomUUID();
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

}
