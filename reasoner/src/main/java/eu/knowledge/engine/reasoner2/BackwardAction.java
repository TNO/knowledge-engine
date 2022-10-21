/**
 * 
 */
package eu.knowledge.engine.reasoner2;

import java.util.Set;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.reasoningnode.RuleNode;

/**
 * @author nouwtb
 *
 */
public class BackwardAction {

	private RuleNode startNode;
	private Set<RuleNode> visitedFilterBS;
	private Set<RuleNode> readyResultBS;
	// private Set<TaskBoard.Task> tasksOnBoard;
	private TripleVarBindingSet readyResultBindingSet;
	private RuleNode currentNode;

	public BackwardAction(RuleNode aStartNode) {

	}

	public void start() {

	}

}
