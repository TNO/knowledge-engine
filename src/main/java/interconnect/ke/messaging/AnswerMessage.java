package interconnect.ke.messaging;

import java.net.URI;
import java.util.UUID;

import interconnect.ke.api.binding.BindingSet;

public class AnswerMessage extends KnowledgeMessage {

	private UUID replyToAskMessage;
	/**
	 * Bindings for this AnswerMessage. Variable names of the
	 * AnswerKnowledgeInteraction are used.
	 * (Proactive side does the translations)
	 */
	private BindingSet bindings;

	public AnswerMessage(URI fromKnowledgeBase, URI fromKnowledgeInteraction, URI toKnowledgeBase,
			URI toKnowledgeInteraction, UUID replyToAskMessage, BindingSet bindings) {
		super(fromKnowledgeBase, fromKnowledgeInteraction, toKnowledgeBase, toKnowledgeInteraction);
		this.replyToAskMessage = replyToAskMessage;
		this.bindings = bindings;
	}

	public UUID getReplyToAskMessage() {
		return replyToAskMessage;
	}

	public BindingSet getBindings() {
		return bindings;
	}

	@Override
	public String toString() {
		return "AnswerMessage [replyToAskMessage=" + replyToAskMessage + ", bindings=" + bindings + ", messageId="
				+ messageId + ", fromKnowledgeBase=" + fromKnowledgeBase + ", fromKnowledgeInteraction="
				+ fromKnowledgeInteraction + ", toKnowledgeBase=" + toKnowledgeBase + ", toKnowledgeInteraction="
				+ toKnowledgeInteraction + "]";
	}

}
