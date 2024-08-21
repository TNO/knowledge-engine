package eu.knowledge.engine.smartconnector.api;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.api.TriplePattern;

/**
 * This class contains the plan of the Smart Connector for executing a
 * particular Ask KI.
 * 
 * @author nouwtb
 *
 */
public interface AskPlan {

	/**
	 * Execute the plan with the given bindingset.
	 * 
	 * @param bindingSet
	 * @return the result of the plan with additional information about the
	 *         execution of the plan.
	 */
	public CompletableFuture<AskResult> execute(BindingSet bindingSet);

	/**
	 * Get detailed information about the plan including other KBs involved and
	 * mappings, etc.
	 * 
	 * @return A reasoner plan that forms the reasoning graph, or {@code null} if
	 *         reasoning was disabled.
	 */
	public ReasonerPlan getReasonerPlan();

	/**
	 * Get the possible knowledge gaps in the reasoning plan
	 * 
	 * @return A set of knowledge gaps in the reasoning plan, or {@code null} if
	 *         reasoning was disabled.
	 */
	public Set<Set<TriplePattern>> findKnowledgeGaps();
}
