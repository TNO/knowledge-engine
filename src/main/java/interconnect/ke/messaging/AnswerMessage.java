package interconnect.ke.messaging;

import java.util.Set;
import java.util.UUID;

import interconnect.ke.api.Bindings;

public class AnswerMessage extends KnowledgeMessage {

	private UUID replyToAskMessage;
	private Set<Bindings> bindings;
	
	// TODO toString()
	
}
