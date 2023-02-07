package eu.knowledge.engine.reasoner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulenode.ActiveAntRuleNode;
import eu.knowledge.engine.reasoner.rulenode.ActiveConsRuleNode;
import eu.knowledge.engine.reasoner.rulenode.FullRuleNode;
import eu.knowledge.engine.reasoner.rulenode.PassiveAntRuleNode;
import eu.knowledge.engine.reasoner.rulenode.PassiveConsRuleNode;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

/**
 * Decision: BindingSets relating to the start node are handled via the
 * {@link #getResults()} and {@link #execute(BindingSet)} methods. Non-startnode
 * binding sets should be retrieved by the caller. See
 * {@link ForwardTest#test()} for an example.
 * 
 */
public class ReasonerPlan {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerPlan.class);
	private final RuleStore store;
	private final ProactiveRule start;
	private final Map<BaseRule, RuleNode> ruleToRuleNode;
	private boolean done;
	private MatchStrategy strategy = MatchStrategy.FIND_ALL_MATCHES;
	private boolean useTaskBoard = true;

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {
		this.store = aStore;
		this.start = aStartRule;
		this.ruleToRuleNode = new HashMap<>();
		createOrGetReasonerNode(this.start, null);
	}

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule, MatchStrategy aStrategy) {
		this.store = aStore;
		this.start = aStartRule;
		this.ruleToRuleNode = new HashMap<>();
		this.strategy = aStrategy;
		createOrGetReasonerNode(this.start, null);
	}

	/**
	 * Enable (default) or disable the {@link TaskBoard}. When it is disabled, all
	 * tasks will be executed as they occur in the algorithm, and an EMPTY task
	 * board is returned in {@link #execute}, which means that a single call to
	 * {@link #execute} suffices to terminate the algorithm. When it is enabled (the
	 * default), deferrable tasks will be put on the {@link TaskBoard}, and the
	 * caller of {@link #execute} is responsible to complete them at their leisure
	 * before calling {@link #execute} again.
	 * 
	 * @param aUseTaskBoard
	 */
	public void setUseTaskBoard(boolean aUseTaskBoard) {
		this.useTaskBoard = aUseTaskBoard;
	}

	public RuleNode getStartNode() {
		return this.ruleToRuleNode.get(this.start);
	}

	public RuleNode getRuleNodeForRule(BaseRule rule) {
		return this.ruleToRuleNode.get(rule);
	}

	public TaskBoard execute(BindingSet bindingSet) {

		if (bindingSet.isEmpty())
			bindingSet.add(new Binding());

		RuleNode startNode = this.getStartNode();
		TaskBoard taskBoard = new TaskBoard();

		if (this.isBackward()) {
			assert startNode instanceof PassiveAntRuleNode;
			((PassiveAntRuleNode) startNode).setFilterBindingSetOutput(bindingSet);
		} else {
			assert startNode instanceof PassiveConsRuleNode;
			((PassiveConsRuleNode) startNode).setResultBindingOutput(bindingSet);
		}

		Deque<RuleNode> stack = new ArrayDeque<>();
		Set<RuleNode> visited = new HashSet<>();
		Set<RuleNode> changed = new HashSet<>();

		do {
			stack.clear();
			visited.clear();
			changed.clear();

			stack.push(startNode);

			while (!stack.isEmpty()) {
				final RuleNode current = stack.pop();
				LOG.trace("Processing {}", current);

				current.getAllNeighbours().stream().filter(n -> !stack.contains(n)).filter(n -> !visited.contains(n))
						.filter(n -> !n.equals(current)).forEach(n -> stack.push(n));

				if (current.readyForTransformFilter()) {
					current.transformFilterBS();
				}

				// Ready, and current version of input has not been scheduled on taskboard? ->
				// Add to taskboard otherwise -> Do not add to taskboard
				if (current.readyForApplyRule() && !current.isResultBindingSetInputScheduled()) {
					this.scheduleOrDoTask(current, taskBoard);
					current.setResultBindingSetInputScheduled(true);
				}

				TripleVarBindingSet toBeFilterPropagated = current.getFilterBindingSetOutput();
				if (toBeFilterPropagated != null) {
					assert current instanceof AntSide;
					((AntSide) current).getAntecedentNeighbours().forEach((n, matches) -> {
						var translated = toBeFilterPropagated.translate(n.getRule().getConsequent(),
								Match.invertAll(matches));
						boolean itChanged = ((ConsSide) n).addFilterBindingSetInput(current, translated);
						if (itChanged) {
							changed.add(n);
						}
					});
				}

				TripleVarBindingSet toBeResultPropagated = current.getResultBindingSetOutput();
				if (toBeResultPropagated != null) {
					assert current instanceof ConsSide;
					((ConsSide) current).getConsequentNeighbours().forEach((n, matches) -> {
						var translated = toBeResultPropagated.translate(n.getRule().getAntecedent(),
								Match.invertAll(matches));
						boolean itChanged = ((AntSide) n).addResultBindingSetInput(current, translated);
						if (itChanged) {
							changed.add(n);
							n.setResultBindingSetInputScheduled(false);
						}
					});
				}

				visited.add(current);
			}
		} while (!changed.isEmpty());

		this.done = !taskBoard.hasTasks();
		return taskBoard;
	}

	public boolean isDone() {
		return this.done;
	}

	public BindingSet getResults() {
		if (this.isBackward()) {
			if (this.isDone()) {
				return ((PassiveAntRuleNode) this.getStartNode()).getResultBindingSetInput().getFullBindingSet()
						.toBindingSet();
			} else {
				throw new RuntimeException("`execute` should be finished before getting results.");
			}
		} else {
			throw new RuntimeException("Results should only be read for backward reasoning plans");
		}
	}

	public RuleNode newNode(BaseRule rule) {
		// based on the rule properties, return the appropriate rulenode
		if (!rule.getAntecedent().isEmpty()) {
			if (!rule.getConsequent().isEmpty()) {
				return new FullRuleNode(rule);
			} else {
				// no consequent, yes antecedent
				if (rule.isProactive()) {
					return new PassiveAntRuleNode(rule);
				} else {
					return new ActiveAntRuleNode(rule);
				}
			}
		} else {
			assert !rule.getConsequent().isEmpty();
			// no antecedent, yes consequent
			if (rule.isProactive()) {
				return new PassiveConsRuleNode(rule);
			} else {
				return new ActiveConsRuleNode(rule);
			}
		}
	}

	public boolean isBackward() {
		return !this.start.getAntecedent().isEmpty() && this.start.getConsequent().isEmpty();
	}

	private RuleNode createOrGetReasonerNode(BaseRule aRule, BaseRule aParent) {

		final RuleNode reasonerNode;
		if (this.ruleToRuleNode.containsKey(aRule))
			return this.ruleToRuleNode.get(aRule);
		else {
			reasonerNode = this.newNode(aRule);
		}

		// build the reasoner node graph
		this.ruleToRuleNode.put(aRule, reasonerNode);

		if (isBackward()) {
			// for now we only are interested in antecedent neighbors.
			// TODO for looping we DO want to consider consequent neighbors as well.

			this.store.getAntecedentNeighbors(aRule, this.strategy).forEach((rule, matches) -> {
				if (!(rule instanceof ProactiveRule)) {
					assert reasonerNode instanceof AntSide;
					var newNode = createOrGetReasonerNode(rule, aRule);
					assert newNode instanceof ConsSide;
					((AntSide) reasonerNode).addAntecedentNeighbour(newNode, matches);

					var inverseMatches = Match.invertAll(matches);
					// TODO: Validate with Barry if we can use the same `matches` object here
					((ConsSide) newNode).addConsequentNeighbour(reasonerNode, inverseMatches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
			});
		} else {
			// interested in both consequent and antecedent neighbors
			this.store.getConsequentNeighbors(aRule, this.strategy).forEach((rule, matches) -> {
				if (!(rule instanceof ProactiveRule)) {
					assert reasonerNode instanceof ConsSide;
					var newNode = createOrGetReasonerNode(rule, aRule);
					((ConsSide) reasonerNode).addConsequentNeighbour(newNode, matches);

					var inverseMatches = Match.invertAll(matches);
					// TODO: Validate with Barry if we can use the same `matches` object here
					((AntSide) newNode).addAntecedentNeighbour(reasonerNode, inverseMatches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
			});

			// antecedent neighbors to propagate bindings further via backward chaining

			// determine whether our parent matches us partially
			boolean ourAntecedentFullyMatchesParentConsequent = false;

			if (aParent != null && this.store.getAntecedentNeighbors(aRule, this.strategy).containsKey(aParent)) {
				ourAntecedentFullyMatchesParentConsequent = antecedentFullyMatchesConsequent(aRule.getAntecedent(),
						aParent.getConsequent(), this.getMatchStrategy());
			}

			if (!ourAntecedentFullyMatchesParentConsequent) {
				this.store.getAntecedentNeighbors(aRule, this.strategy).forEach((rule, matches) -> {
					if (!(rule instanceof ProactiveRule)) {
						assert reasonerNode instanceof AntSide;
						var newNode = createOrGetReasonerNode(rule, aRule);
						assert newNode instanceof ConsSide;
						((AntSide) reasonerNode).addAntecedentNeighbour(newNode, matches);

						// TODO: Validate with Barry if we can use the same `matches` object here
						var inverseMatches = Match.invertAll(matches);
						((ConsSide) newNode).addConsequentNeighbour(reasonerNode, inverseMatches);
					} else {
						LOG.trace("Skipped proactive rule: {}", rule);
					}
				});
			}
		}

		return reasonerNode;
	}

	private void scheduleOrDoTask(RuleNode current, TaskBoard taskBoard) {
		if (this.useTaskBoard) {
			taskBoard.addTask(current);
			current.setResultBindingSetInputScheduled(true);
		} else {
			try {
				current.applyRule().get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				// since we only disable the taskboard when debugging, it is fine to
				// throw a RuntimeException here.
				throw new RuntimeException(String.format("Interrupted while processing node %s", current));
			}
		}
	}

	/**
	 * Checks whether the given antecedent fully matches the given consequent. Note
	 * that if the antecedent is a subset of the consequent this method also return
	 * true.
	 * 
	 * @param consequent
	 * @param antecedent
	 * @return
	 */
	private boolean antecedentFullyMatchesConsequent(Set<TriplePattern> antecedent, Set<TriplePattern> consequent,
			MatchStrategy aMatchStrategy) {

		assert !antecedent.isEmpty();
		assert !consequent.isEmpty();

		if (antecedent.size() > consequent.size())
			return false;

		Set<Match> matches = BaseRule.matches(antecedent, consequent, aMatchStrategy);

		for (Match m : matches) {
			// check if there is a match that is full
			boolean allFound = true;
			for (TriplePattern tp : antecedent) {
				boolean foundOne = false;
				for (Map.Entry<TriplePattern, TriplePattern> entry : m.getMatchingPatterns().entrySet()) {
					if (entry.getValue().findMatches(tp) != null) {
						foundOne = true;
					}
				}
				allFound &= foundOne;
			}

			if (allFound)
				return true;
		}

		return false;
	}

	public MatchStrategy getMatchStrategy() {
		return this.strategy;
	}

	public RuleStore getStore() {
		return this.store;
	}
}
