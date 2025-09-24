package eu.knowledge.engine.reasoner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
import eu.knowledge.engine.reasoner.BaseRule.MatchFlag;
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
	private EnumSet<MatchFlag> matchConfig = EnumSet.noneOf(MatchFlag.class);
	private boolean useTaskBoard = true;

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {
		this.store = aStore;
		this.start = aStartRule;
		this.ruleToRuleNode = new HashMap<>();
		createOrGetRuleNode(this.start);
	}

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule, EnumSet<MatchFlag> aConfig) {
		this.store = aStore;
		this.start = aStartRule;
		this.ruleToRuleNode = new HashMap<>();
		this.matchConfig = aConfig;
		createOrGetRuleNode(this.start);
	}

	private RuleNode createOrGetRuleNode(BaseRule aRule) {

		final RuleNode currentRuleNode;
		if (this.ruleToRuleNode.containsKey(aRule))
			return this.ruleToRuleNode.get(aRule);
		else {
			currentRuleNode = this.newNode(aRule);
		}

		// build the reasoner node graph
		this.ruleToRuleNode.put(aRule, currentRuleNode);

		if (isBackward()) {
			// we are only interested in antecedent neighbors.
			this.store.getAntecedentNeighbors(aRule, this.matchConfig).forEach((rule, matches) -> {
				if (!(rule instanceof ProactiveRule)) {
					assert currentRuleNode instanceof AntSide;
					var newNode = createOrGetRuleNode(rule);
					assert newNode instanceof ConsSide;
					((AntSide) currentRuleNode).addAntecedentNeighbour(newNode, matches);

					var inverseMatches = Match.invertAll(matches);
					((ConsSide) newNode).addConsequentNeighbour(currentRuleNode, inverseMatches);
				} else {
					LOG.trace("Skipped proactive rule1: {}", rule);
				}
			});

			// store the combi matches when there are any antecedents.
			if (!currentRuleNode.getRule().getAntecedent().isEmpty())
				((AntSide) currentRuleNode)
						.setAntecedentCombiMatches(this.store.getAntecedentCombiMatches(currentRuleNode.getRule()));
		} else {
			// interested in both consequent and antecedent neighbors

			EnumSet<MatchFlag> modMatchConfig = EnumSet.copyOf(this.matchConfig);
			modMatchConfig.add(MatchFlag.SINGLE_RULE);

			Map<BaseRule, Set<Match>> consequentNeighbors = this.store.getConsequentNeighbors(aRule, this.matchConfig);

			LOG.info("Rule1: {} ({})", aRule, consequentNeighbors.size());
			consequentNeighbors.forEach((rule, matches) -> {
				if (!(rule instanceof ProactiveRule)) {
					assert currentRuleNode instanceof ConsSide;
					var newNode = createOrGetRuleNode(rule);
					((ConsSide) currentRuleNode).addConsequentNeighbour(newNode, matches);

					Set<CombiMatch> antCombiMatches = filterAndInvertCombiMatches(rule, aRule,
							this.store.getConsequentCombiMatches(currentRuleNode.getRule()));

					var existing = ((AntSide) newNode).getAntecedentCombiMatches();

					if (existing == null)
						((AntSide) newNode).setAntecedentCombiMatches(antCombiMatches);
					else
						existing.addAll(antCombiMatches);

					var inverseMatches = Match.invertAll(matches);
					((AntSide) newNode).addAntecedentNeighbour(currentRuleNode, inverseMatches);
				} else {
					LOG.trace("Skipped proactive rule2: {}", rule);
				}
			});

			// store the combi matches when there are any consequents.
			if (!currentRuleNode.getRule().getConsequent().isEmpty()) {
				Set<CombiMatch> existing = ((ConsSide) currentRuleNode).getConsequentCombiMatches();
				Set<CombiMatch> consequentCombiMatches = this.store
						.getConsequentCombiMatches(currentRuleNode.getRule());

				if (existing == null) {
					((ConsSide) currentRuleNode).setConsequentCombiMatches(consequentCombiMatches);
				} else
					existing.addAll(consequentCombiMatches);
			}
			// antecedent neighbors to propagate bindings further via backward chaining

			// determine whether our parent matches us partially
			boolean ourAntecedentFullyMatchesParentConsequent = true;

			Map<BaseRule, Set<Match>> antecedentNeighbors = this.store.getAntecedentNeighbors(aRule, this.matchConfig);
			for (BaseRule neighborRule : antecedentNeighbors.keySet()) {
				if (this.isAncestorOfStartNode(neighborRule)) {
					ourAntecedentFullyMatchesParentConsequent &= antecedentFullyMatchesConsequent(aRule, neighborRule,
							antecedentNeighbors.get(neighborRule));
				}
			}

			if (!ourAntecedentFullyMatchesParentConsequent) {
				antecedentNeighbors.forEach((rule, matches) -> {
					assert currentRuleNode instanceof AntSide;
					var newNode = createOrGetRuleNode(rule);
					assert newNode instanceof ConsSide;
					((AntSide) currentRuleNode).addAntecedentNeighbour(newNode, matches);

					var inverseMatches = Match.invertAll(matches);
					((ConsSide) newNode).addConsequentNeighbour(currentRuleNode, inverseMatches);
				});

				if (!currentRuleNode.getRule().getAntecedent().isEmpty()) {
					var existing = ((AntSide) currentRuleNode).getAntecedentCombiMatches();
					Set<CombiMatch> antecedentCombiMatches = this.store
							.getAntecedentCombiMatches(currentRuleNode.getRule());
					if (existing == null) {
						((AntSide) currentRuleNode).setAntecedentCombiMatches(antecedentCombiMatches);
					} else
						existing.addAll(antecedentCombiMatches);
				}
			}
		}

		return currentRuleNode;
	}

	private boolean isAncestorOfStartNode(BaseRule aRule) {
		return isAncestorOfStartNode(aRule, new ArrayList<BaseRule>());
	}

	/**
	 * Check whether the given BaseRule is an ancestor of the start node of this
	 * reasoning plan.
	 * 
	 * @param aRule The rule to check whether it is an ancestor.
	 * @return {@code true} when {@code aRule} is an ancestor, {@code false}
	 *         otherwise.
	 */
	private boolean isAncestorOfStartNode(BaseRule aRule, List<BaseRule> visited) {

		if (visited.contains(aRule))
			return false;

		visited.add(aRule);

		boolean isAncestor = false;

		if (this.getStartNode().getRule().equals(aRule)) {
			isAncestor = true;
		} else {
			Map<BaseRule, Set<Match>> antecedentNeighbors = this.store.getAntecedentNeighbors(aRule);

			for (BaseRule antecedentNeighbor : antecedentNeighbors.keySet()) {
				isAncestor |= isAncestorOfStartNode(antecedentNeighbor, visited);
			}
		}
		return isAncestor;
	}

	private Set<CombiMatch> filterAndInvertCombiMatches(BaseRule antRule, BaseRule consRule,
			Set<CombiMatch> consequentCombiMatches) {

		Set<CombiMatch> filteredAndInvertedCombiMatches = new HashSet<CombiMatch>();

		CombiMatch newCm;
		for (CombiMatch cm : consequentCombiMatches) {
			if (cm.containsKey(antRule)) {
				newCm = new CombiMatch();
				Set<Match> matches = cm.get(antRule);
				Set<Match> newMatches = new HashSet<Match>();
				for (Match m : matches) {

					if (m.getMatchingPatterns().size() > 0
							&& m.getMatchingPatterns().size() == antRule.getAntecedent().size()) {
						newMatches.add(m.inverse());
					}
				}
				if (!newMatches.isEmpty()) {
					newCm.put(consRule, newMatches);
					filteredAndInvertedCombiMatches.add(newCm);
				}
			}
		}

		return filteredAndInvertedCombiMatches;
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
			LOG.trace("New round.");
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
				if (current.readyForApplyRule() && !current.isResultBindingSetInputAlreadyScheduledOrDone()) {
					this.scheduleOrDoTask(current, taskBoard);
					current.setResultBindingSetInputAlreadyScheduledOrDone(true);
				}

				if (current.shouldPropagateFilterBindingSetOutput()) {
					TripleVarBindingSet toBeFilterPropagated = current.getFilterBindingSetOutput();
					assert current instanceof AntSide;
					((AntSide) current).getAntecedentNeighbours().forEach((n, matches) -> {
						var translated = toBeFilterPropagated.translate(n.getRule().getConsequent(),
								Match.invertAll(matches));

						boolean itChanged = ((ConsSide) n).addFilterBindingSetInput(current, translated);
						if (itChanged) {
							changed.add(n);
						}
					});
					current.setFilterBindingSetOutputPropagated();
				}

				if (current.shouldPropagateResultBindingSetOutput()) {
					TripleVarBindingSet toBeResultPropagated = current.getResultBindingSetOutput();
					assert current instanceof ConsSide;
					((ConsSide) current).getConsequentNeighbours().forEach((n, matches) -> {
						var translated = toBeResultPropagated.translate(n.getRule().getAntecedent(),
								Match.invertAll(matches));

						LOG.trace("EEK: {}", n);

						TripleVarBindingSet beforeBindingSet = n.getResultBindingSetInput();
						boolean itChanged = ((AntSide) n).addResultBindingSetInput(current, translated);
						TripleVarBindingSet afterBindingSet = n.getResultBindingSetInput();
						if (itChanged) {
							changed.add(n);

							// We should only set this to false if the actual binding set of the
							// BindngSetStore.get() method changes. Otherwise rules get applied multiple
							// times with the same binding set.
							if (!beforeBindingSet.equals(afterBindingSet))
								n.setResultBindingSetInputAlreadyScheduledOrDone(false);
						}
					});
					current.setResultBindingSetOutputPropagated();
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

	private void scheduleOrDoTask(RuleNode current, TaskBoard taskBoard) {
		if (this.useTaskBoard) {
			taskBoard.addTask(current);
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
	 * @param consequentRule
	 * @param antecedentRule
	 * @return
	 */
	private boolean antecedentFullyMatchesConsequent(BaseRule antecedentRule, BaseRule consequentRule,
			Set<Match> someMatches) {

		var antecedent = antecedentRule.getAntecedent();
		var consequent = consequentRule.getConsequent();

		assert !antecedent.isEmpty();
		assert !consequent.isEmpty();

		if (antecedent.size() > consequent.size())
			return false;

		for (Match m : someMatches) {
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

	public EnumSet<MatchFlag> getMatchConfig() {
		return this.matchConfig;
	}

	public RuleStore getStore() {
		return this.store;
	}

	@Override
	public String toString() {
		return "ReasonerPlan [link=" + this.store.getGraphVizCode(this, true) + "]";
	}

}
