package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class ReactMessage extends KnowledgeMessage {

	private UUID replyToPostMessage;

	/**
	 * Variable names of the React side are used. (Proactive side does the
	 * translations)
	 */
	private BindingSet result;

	public ReactMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToPostMessage, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
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
		return "ReactMessage [replyToPostMessage=" + replyToPostMessage + ", bindings=" + result + ", messageId="
				+ messageId + ", fromKnowledgeBase=" + fromKnowledgeBase + ", fromKnowledgeInteraction="
				+ fromKnowledgeInteraction + ", toKnowledgeBase=" + toKnowledgeBase + ", toKnowledgeInteraction="
				+ toKnowledgeInteraction + "]";
	}
}
