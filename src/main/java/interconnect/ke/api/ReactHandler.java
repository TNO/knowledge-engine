package interconnect.ke.api;

import interconnect.ke.api.binding.SolutionSet;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

/**
 * A ReactHandler provides a handler method that returns a solution set for the
 * provided input. Unlike in the AnswerHandler, the graph pattern of the result
 * *can* be different from the argument's.
 */
public interface ReactHandler {

	public SolutionSet react(ReactKnowledgeInteraction aReactKnowledgeInteraction, SolutionSet argument);

}
