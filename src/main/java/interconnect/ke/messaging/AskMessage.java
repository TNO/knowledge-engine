package interconnect.ke.messaging;

import java.net.URI;

import interconnect.ke.api.binding.BindingSet;

public class AskMessage extends KnowledgeMessage {

	/**
	 * Bindings for this AskMessage. Variable names of the
	 * AnswerKnowledgeInteraction are used.
	 */
	private BindingSet bindings;

	public AskMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.bindings = bindings;
	}

	public BindingSet getBindings() {
		return bindings;
	}

	@Override
	public String toString() {
		return "AskMessage [bindings=" + bindings + ", messageId=" + messageId + ", fromKnowledgeBase="
				+ fromKnowledgeBase + ", fromKnowledgeInteraction=" + fromKnowledgeInteraction + ", toKnowledgeBase="
				+ toKnowledgeBase + ", toKnowledgeInteraction=" + toKnowledgeInteraction + "]";
	}

}
