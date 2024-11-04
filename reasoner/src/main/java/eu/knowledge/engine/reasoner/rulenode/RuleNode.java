package eu.knowledge.engine.reasoner.rulenode;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import eu.knowledge.engine.reasoner.AntSide;
import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.ProactiveRule;
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

	public void setResultBindingSetInputAlreadyScheduledOrDone(boolean b) {
		this.resultBindingSetOutputScheduled = b;
	}

	public boolean isResultBindingSetInputAlreadyScheduledOrDone() {
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
						if (m.getMatchingPatterns().values().contains(tp)) {
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

	/**
	 * Follows the antecedents of this neighbor until they are all visited. If one
	 * of the antecedent neighbors is a ProactiveRule returns {@code true}, {@code
	 * false} otherwise.
	 */
	protected boolean hasProactiveParent(RuleNode aNeighbor) {

		Set<RuleNode> visited = new HashSet<>();
		Deque<RuleNode> stack = new LinkedList<>();

		stack.push(aNeighbor);

		while (!stack.isEmpty()) {
			var current = stack.pop();
			if (visited.contains(current))
				continue;

			if (aNeighbor.getRule() instanceof ProactiveRule) {
				return true;
			} else if (aNeighbor instanceof AntSide) {
				stack.addAll(((AntSide) aNeighbor).getAntecedentNeighbours().keySet());
			}
			visited.add(current);
		}

		return false;
	}

}
