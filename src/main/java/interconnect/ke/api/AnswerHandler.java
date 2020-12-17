package interconnect.ke.api;

import interconnect.ke.api.binding.SolutionSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;

/**
 * An AnswerHandler provides a handler method that returns a solution set for
 * the provided input.
 */
public interface AnswerHandler {
	/**
	 * Returns all solution sets that match this knowledge interaction's graph
	 * pattern, AND matches ANY of the input SolutionSet. All bindings in the
	 * graph pattern's solution set must be bound, including the ones that were
	 * provided by the input.
	 */
	public SolutionSet answer(AnswerKnowledgeInteraction anAKI, SolutionSet aSolution);
}
