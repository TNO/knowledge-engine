package interconnect.ke.api;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;

/**
 * An AnswerHandler provides a handler method that returns a solution set for
 * the provided input.
 */
public interface AnswerHandler {
	 /**
		* @param anAKI The knowledge interaction that is involved in the question.
		* @param aBindingSet A set of bindings for variables in the knowledge
		* interaction's graph pattern.
		* @return All solution sets that match this knowledge interaction's graph
		* pattern, AND matches ANY of the input BindingSet. All variables from the
		* graph pattern must be bound, including the ones that were already provided
		* in the input BindingSet.
	  */
	public BindingSet answer(AnswerKnowledgeInteraction anAKI, BindingSet aBindingSet);
}
