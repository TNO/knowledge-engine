package interconnect.ke.messaging;

import java.util.Set;

import interconnect.ke.api.Bindings;

public class AskMessage extends KnowledgeMessage {
	
	/**
	 * Bindings for this AskMessage. Variable names of the AnswerKnowledgeInteraction are used.
	 */
	private Set<Bindings> bindings; // TODO terminology

}
