package interconnect.ke.api;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

/**
 * A ReactHandler provides a handler method that returns a solution set for the
 * provided input. Unlike in the AnswerHandler, the graph pattern of the result
 * *can* be different from the argument's.
 */
public interface ReactHandler {
	/**
	 * @param anRKI The knowledge interaction that is involved in this post/react
	 * process.
	 * @param argument The set of bindings for variables in the knowledge
	 * interaction's argument graph pattern.
	 * @return A set of bindings for variables in the knowledge interaction's result graph pattern.
	 */
	public BindingSet react(ReactKnowledgeInteraction anRKI, BindingSet argument);
}
