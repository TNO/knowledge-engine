package interconnect.ke.messaging;

import java.net.URI;

import interconnect.ke.api.binding.BindingSet;

public class PostMessage extends KnowledgeMessage {

	private BindingSet argument;

	public PostMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.argument = bindings;
	}

	public BindingSet getBindings() {
		return argument;
	}

	@Override
	public String toString() {
		return "PostMessage [bindings=" + argument + ", messageId=" + messageId + ", fromKnowledgeBase="
				+ fromKnowledgeBase + ", fromKnowledgeInteraction=" + fromKnowledgeInteraction + ", toKnowledgeBase="
				+ toKnowledgeBase + ", toKnowledgeInteraction=" + toKnowledgeInteraction + "]";
	}

}
