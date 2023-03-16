package eu.knowledge.engine.reasoner.rulenode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public abstract class RuleNode {

	private BaseRule rule;
	private boolean resultBindingSetOutputScheduled = false;
	private Map<TriplePattern, Set<RuleNode>> antecedentCoverageCache;
	protected boolean isFilterBindingSetOutputDirty = false;
	protected boolean isResultBindingSetOutputDirty = false;

	public RuleNode(BaseRule aRule) {
		this.rule = aRule;
	}

	public BaseRule getRule() {
		return rule;
	}

	public abstract Set<RuleNode> getAllNeighbours();

	public abstract Future<Void> applyRule();

	public abstract boolean readyForApplyRule();

	public abstract TripleVarBindingSet getResultBindingSetInput();

	public abstract TripleVarBindingSet getResultBindingSetOutput();

	public abstract boolean readyForTransformFilter();

	public abstract void transformFilterBS();

	public abstract TripleVarBindingSet getFilterBindingSetInput();

	public abstract TripleVarBindingSet getFilterBindingSetOutput();

	public abstract Set<RuleNode> getAllSameLoopNeighbors();

	public void setResultBindingSetInputScheduled(boolean b) {
		this.resultBindingSetOutputScheduled = b;
	}

	public boolean isResultBindingSetInputScheduled() {
		return this.resultBindingSetOutputScheduled;
	}

	@Override
	public String toString() {
		return "RN " + this.rule.toString();
	}

	public Map<TriplePattern, Set<RuleNode>> findAntecedentCoverage(Map<RuleNode, Set<Match>> someAntecedentNeighbors) {
		if (this.antecedentCoverageCache == null) {
			antecedentCoverageCache = new HashMap<>();

			// find the coverage
			Set<RuleNode> coveringNodes;
			for (TriplePattern tp : this.getRule().getAntecedent()) {
				coveringNodes = new HashSet<>();
				antecedentCoverageCache.put(tp, coveringNodes);

				for (Entry<RuleNode, Set<Match>> entry : someAntecedentNeighbors.entrySet()) {
					for (Match m : entry.getValue()) {
						if (m.getMatchingPatterns().keySet().contains(tp)) {
							coveringNodes.add(entry.getKey());
							break; // where does this break from? The inner loop.
						}
					}
				}

			}
		}
		return this.antecedentCoverageCache;
	}

	/**
	 * @return true if the filter bindingset output has changed and was not yet
	 *         propagated, false otherwise.
	 */
	public abstract boolean shouldPropagateFilterBindingSetOutput();

	/**
	 * register that the current filterbindingsetoutput was propagated.
	 */
	public void setFilterBindingSetOutputPropagated() {
		this.isFilterBindingSetOutputDirty = false;
	}

	/**
	 * @return true if the result bindingset output has changed and was not yet
	 *         propagated, false otherwise.
	 */
	public abstract boolean shouldPropagateResultBindingSetOutput();

	/**
	 * register that the current resultbindingsetoutput was propagated.
	 */
	public void setResultBindingSetOutputPropagated() {
		this.isResultBindingSetOutputDirty = false;
	}

}
