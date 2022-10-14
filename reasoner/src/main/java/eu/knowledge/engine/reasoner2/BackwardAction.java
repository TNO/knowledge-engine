/**
 * 
 */
package eu.knowledge.engine.reasoner2;

import java.util.Set;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.reasoningnode.ReasoningNode;

/**
 * @author nouwtb
 *
 */
public class BackwardAction {

	private ReasoningNode startNode;
	private Set<ReasoningNode> visitedFilterBS;
	private Set<ReasoningNode> readyResultBS;
	// private Set<TaskBoard.Task> tasksOnBoard;
	private TripleVarBindingSet readyResultBindingSet;
	private ReasoningNode currentNode;

	public BackwardAction(ReasoningNode aStartNode) {

	}

	public void start() {

	}

}
