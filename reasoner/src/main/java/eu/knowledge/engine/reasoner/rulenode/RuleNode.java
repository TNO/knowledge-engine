package eu.knowledge.engine.reasoner.rulenode;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public abstract class RuleNode {

	private BaseRule rule;
	private boolean executeViaTaskboard;
	private boolean resultBindingSetOutputScheduled = false;

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

	public abstract Future<Void> applyRule();

	public abstract boolean readyForApplyRule();

	public abstract TripleVarBindingSet getResultBindingSetOutput();

	public abstract boolean readyForTransformFilter();

	public abstract void transformFilterBS();

	public abstract TripleVarBindingSet getFilterBindingSetOutput();

	public abstract Set<RuleNode> getAllSameLoopNeighbors();

	public abstract void resetResultBindingSetOutput();

	public void setResultBindingSetInputScheduled(boolean b) {
		this.resultBindingSetOutputScheduled = b;
	}

	public boolean isResultBindingSetInputScheduled() {
		return this.resultBindingSetOutputScheduled;
	}

	@Override
	public String toString() {
		return "RuleNode for " + this.rule.toString();
	}

}
