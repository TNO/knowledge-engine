package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class PostMessage extends KnowledgeMessage {

	/**
	 * Variable names of the React side are used.
	 * (Proactive side does the translations)
	 */
	private BindingSet argument;

	public PostMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
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
