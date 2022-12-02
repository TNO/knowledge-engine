package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public abstract class RuleNode {

	private BaseRule rule;
	private boolean executeViaTaskboard;

	public RuleNode(BaseRule aRule) {
		this.rule = aRule;
	}

	public BaseRule getRule() {
		return rule;
	}

	public abstract Set<RuleNode> getAllNeighbours();

	public boolean getExecuteViaTaskboard() {
		return executeViaTaskboard;
	}

	public abstract void applyRule();

	public abstract boolean readyForApplyRule();

	public abstract TripleVarBindingSet getResultBindingSetOutput();

	public abstract boolean readyForTransformFilter();

	public abstract void transformFilterBS();

	public abstract TripleVarBindingSet getFilterBindingSetOutput();

	public abstract Set<RuleNode> getAllSameLoopNeighbors();

}
