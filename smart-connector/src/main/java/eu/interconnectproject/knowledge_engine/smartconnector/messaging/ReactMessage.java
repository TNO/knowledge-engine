package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class ReactMessage extends KnowledgeMessage {

	private UUID replyToPostMessage;
	private BindingSet bindings;

	public ReactMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToPostMessage = replyToPostMessage;
		this.bindings = bindings;
	}

	public UUID getReplyToPostMessage() {
		return replyToPostMessage;
	}

	public BindingSet getBindings() {
		return bindings;
	}

	@Override
	public String toString() {
		return "ReactMessage [replyToPostMessage=" + replyToPostMessage + ", bindings=" + bindings + ", messageId="
				+ messageId + ", fromKnowledgeBase=" + fromKnowledgeBase + ", fromKnowledgeInteraction="
				+ fromKnowledgeInteraction + ", toKnowledgeBase=" + toKnowledgeBase + ", toKnowledgeInteraction="
				+ toKnowledgeInteraction + "]";
	}

}
