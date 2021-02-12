package interconnect.ke.messaging;

import java.net.URI;
import java.util.UUID;

import interconnect.ke.api.binding.BindingSet;

public class ReactMessage extends KnowledgeMessage {

	private UUID replyToPostMessage;
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

	public BindingSet getBindings() {
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
