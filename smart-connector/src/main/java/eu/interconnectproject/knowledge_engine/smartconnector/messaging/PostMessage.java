package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.net.URI;

import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;

public class PostMessage extends KnowledgeMessage {

	private BindingSet bindings;

	public PostMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.bindings = bindings;
	}

	public BindingSet getBindings() {
		return bindings;
	}

	@Override
	public String toString() {
		return "PostMessage [bindings=" + bindings + ", messageId=" + messageId + ", fromKnowledgeBase="
				+ fromKnowledgeBase + ", fromKnowledgeInteraction=" + fromKnowledgeInteraction + ", toKnowledgeBase="
				+ toKnowledgeBase + ", toKnowledgeInteraction=" + toKnowledgeInteraction + "]";
	}

}
