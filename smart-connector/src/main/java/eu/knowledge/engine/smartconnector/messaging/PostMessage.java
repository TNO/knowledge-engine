package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;
import java.util.UUID;

import eu.knowledge.engine.smartconnector.api.BindingSet;

public class PostMessage extends KnowledgeMessage {

	/**
	 * Variable names of the React side are used. (Proactive side does the
	 * translations)
	 */
	private final BindingSet argument;

	public PostMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.argument = bindings;
	}

	public PostMessage(UUID messageId, URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(messageId, fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.argument = bindings;
	}

	public BindingSet getArgument() {
		return argument;
	}

	@Override
	public String toString() {
		return "PostMessage [getToKnowledgeBase()=" + getToKnowledgeBase() + ", getToKnowledgeInteraction()="
				+ getToKnowledgeInteraction() + ", getFromKnowledgeBase()=" + getFromKnowledgeBase()
				+ ", getFromKnowledgeInteraction()=" + getFromKnowledgeInteraction() + ", getMessageId()="
				+ getMessageId() + ", getArgument()=" + getArgument() + "]";
	}

}
