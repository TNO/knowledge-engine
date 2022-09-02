package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.ReasonerPlan;

/**
 * This class reprsents a plan for executing a post knowledge interaction.
 * 
 * @author nouwtb
 *
 */
public interface PostPlan {

	/**
	 * Execute the plan with the given arguments.
	 * 
	 * @param someArguments
	 * @return The result of executing the plan with more information about the
	 *         execution of the plan.
	 */
	public CompletableFuture<PostResult> execute(BindingSet someArguments);

	/**
	 * Get detailed information about the plan, including KBs involved and their
	 * mappings.
	 * 
	 * @return A ReasoningNode with additional information, or {@code null} if the
	 *         reasoner was disabled.
	 */
	public ReasonerPlan getReasonerPlan();

}
