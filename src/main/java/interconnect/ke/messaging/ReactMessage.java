package interconnect.ke.messaging;

import java.util.UUID;

import interconnect.ke.api.binding.BindingSet;

public class ReactMessage extends KnowledgeMessage {

	private UUID replyToPostMessage;
	private BindingSet bindings;

}
