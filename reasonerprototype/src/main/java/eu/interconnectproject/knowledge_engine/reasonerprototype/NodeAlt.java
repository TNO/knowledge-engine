package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.GraphBindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Variable;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TripleVar;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TripleVarBinding;

public class NodeAlt {

	private Map<NodeAlt, Set<Map<TriplePattern, TriplePattern>>> children;

	private List<RuleAlt> allRules;

	/**
	 * Goals are represented as rules with an antecedent without a consequent.
	 */
	private RuleAlt rule;

	private static final int BINDINGSET_NOT_REQUESTED = 0, BINDINGSET_REQUESTED = 1, BINDINGSET_AVAILABLE = 2;

	/**
	 * This is the bindingset that has been retrieved and is now ready to be used to
	 * continue the reason process.
	 */
	private BindingSet resultingBindingSet;

	/**
	 * This state keeps track of whether the procedural code (if there is one) to
	 * remotely access other KBs has already been requested or already available.
	 */
	private int state = BINDINGSET_NOT_REQUESTED;

	public NodeAlt(List<RuleAlt> someRules, NodeAlt aParent, RuleAlt aRule) {
		this.allRules = someRules;
		this.rule = aRule;
		this.children = new HashMap<NodeAlt, Set<Map<TriplePattern, TriplePattern>>>();

		// generate children
		Map<RuleAlt, Set<Map<TriplePattern, TriplePattern>>> relevantRules = findRulesWithOverlappingConsequences(
				this.rule.antecedent);

		NodeAlt child;
		for (Map.Entry<RuleAlt, Set<Map<TriplePattern, TriplePattern>>> entry : relevantRules.entrySet()) {
			// create a childnode
			child = new NodeAlt(this.allRules, this, entry.getKey());
			this.children.put(child, entry.getValue());
		}

		// TODO we need some ininite loop detection (see transitivity rule)
	}

	private Map<RuleAlt, Set<Map<TriplePattern, TriplePattern>>> findRulesWithOverlappingConsequences(
			Set<TriplePattern> aGoal) {

		Set<Map<TriplePattern, TriplePattern>> possibleMatches;
		Map<RuleAlt, Set<Map<TriplePattern, TriplePattern>>> overlappingRules = new HashMap<>();
		for (RuleAlt r : this.allRules) {

			if (!(possibleMatches = r.consequentMatches(aGoal)).isEmpty()) {
				overlappingRules.put(r, possibleMatches);
			}
		}
		return overlappingRules;

	}

	/**
	 * Continues with the reasoning process. Each time this method is called, the
	 * reasoning process is try to continue. Note that this method expects the
	 * pending tasks to be executing between subsequent tasks to this method. This
	 * method will schedule additional tasks that need to be executed again before
	 * it can continue with the reasoning process.
	 * 
	 * @param bindingSet the incoming binding set coming from the consequence.
	 * @return complete bindingset when finished, otherwise null.
	 */
	public BindingSet continueReasoning(BindingSet bindingSet) {

		// send the binding set down the tree and combine the resulting bindingsets to
		// the actual result.

		if (this.state != BINDINGSET_AVAILABLE) {

			boolean allBindingSetsAvailable = true;

			Set<NodeAlt> someChildren = this.children.keySet();

			// we transfer the incomingBindingSet (from the consequent) to the antecedent
			// bindingset (if it exists). This latter one is send to the different children.
			GraphBindingSet antecedentPredefinedBindings;
			antecedentPredefinedBindings = new GraphBindingSet(this.rule.antecedent);
			TripleVarBinding aTripleVarBinding;
			for (Binding b : bindingSet) {
				aTripleVarBinding = new TripleVarBinding(this.rule.antecedent, b);
				if (!aTripleVarBinding.isEmpty())
					antecedentPredefinedBindings.add(aTripleVarBinding);
			}

			// include the allBindingSetsAvailable into the while condition, to process a
			// single node of the tree a time. This makes sure that the results from the
			// first child are send to the second child to check. Otherwise, they get
			// combined and end up in the result without anyone ever checking them.

			// TODO the order in which we iterate the children is important. A strategy like
			// first processing the children that have the incoming variables in their
			// consequent might be smart.
			GraphBindingSet combinedBindings = new GraphBindingSet(this.rule.antecedent);

			Iterator<NodeAlt> someIter = someChildren.iterator();
			while (/* allBindingSetsAvailable && */someIter.hasNext()) {
				NodeAlt child = someIter.next();
				Set<Map<TriplePattern, TriplePattern>> childMatch = this.children.get(child);

				// we can combine all different matches of this child's consequent into a single
				// bindingset. We do not want to send bindings with variables that do not exist
				// for that child.

				GraphBindingSet preparedBindings1 = combinedBindings.getPartialBindingSet();
				GraphBindingSet preparedBindings2 = preparedBindings1.merge(antecedentPredefinedBindings);

				BindingSet childBindings = child
						.continueReasoning(preparedBindings2.toBindingSet().translate(childMatch));
				if (childBindings == null) {
					allBindingSetsAvailable = false;
				} else {
					GraphBindingSet convertedChildBindings = childBindings.toGraphBindingSet(child.rule.consequent)
							.translate(invert(childMatch));

					combinedBindings = combinedBindings.merge(convertedChildBindings);
				}
			}

			if (allBindingSetsAvailable) {

				GraphBindingSet consequentAntecedentBindings;
				if (this.children.isEmpty()) {
					consequentAntecedentBindings = bindingSet.toGraphBindingSet(this.rule.consequent);
				} else {
					consequentAntecedentBindings = keepOnlyCompatiblePatternBindings(
							bindingSet.toGraphBindingSet(this.rule.antecedent), combinedBindings);
					consequentAntecedentBindings = keepOnlyFullGraphPatternBindings(this.rule.antecedent,
							consequentAntecedentBindings);
				}

				if (false) {
					if (this.rule.antecedent.isEmpty() || !consequentAntecedentBindings.isEmpty()) {

						TaskBoard.instance().addTask(this, consequentAntecedentBindings.toBindingSet());
						this.state = BINDINGSET_REQUESTED;
					} else {
						this.resultingBindingSet = new BindingSet();
						this.state = BINDINGSET_AVAILABLE;
					}
				} else {

					// call the handler directly, to make debugging easier.

					if (this.rule.antecedent.isEmpty() || !consequentAntecedentBindings.isEmpty()) {

						this.resultingBindingSet = this.rule.getBindingSetHandler()
								.handle(consequentAntecedentBindings.toBindingSet());
						this.state = BINDINGSET_AVAILABLE;
					} else {
						this.resultingBindingSet = new BindingSet();
						this.state = BINDINGSET_AVAILABLE;
					}

				}
			}

			return null;
		} else {
			assert resultingBindingSet != null;

			// TODO internally the bindingset should contain bindings with variables where
			// instead of having a single entry per variable name, we need an entry per
			// variable occurrence. This is to make sure that different predicates going to
			// the same two variables are still correctly matched.
			// we start however, with the naive version that does not support this.
			return this.resultingBindingSet;
		}
	}

	/**
	 * Only keep those bindings in bindingSet2 that are compatible with the bindings
	 * in bindingSet.
	 * 
	 * @param bindingSet
	 * @param bindingSet2
	 * @return
	 */
	private GraphBindingSet keepOnlyCompatiblePatternBindings(GraphBindingSet bindingSet, GraphBindingSet bindingSet2) {

		GraphBindingSet newBS = new GraphBindingSet(bindingSet2.getGraphPattern());

		for (TripleVarBinding b : bindingSet2.getBindings()) {

			if (!bindingSet.isEmpty()) {
				for (TripleVarBinding b2 : bindingSet.getBindings()) {

					if (b.isOverlapping(b2) && !b.isConflicting(b2)) {
						newBS.add(b);
					}
				}
			} else {
				newBS.add(b);
			}
		}

		return newBS;
	}

	private GraphBindingSet keepOnlyFullGraphPatternBindings(Set<TriplePattern> graphPattern,
			GraphBindingSet someBindings) {
		GraphBindingSet bs = new GraphBindingSet(someBindings.getGraphPattern());
		for (TripleVarBinding b : someBindings.getBindings()) {
			if (isFullBinding(graphPattern, b)) {
				bs.add(new TripleVarBinding(b));
			}
		}
		return bs;
	}

	private boolean isFullBinding(Set<TriplePattern> graphPattern, TripleVarBinding b) {
		return b.keySet().containsAll(getTripleVars(graphPattern));
	}

	public Set<TripleVar> getTripleVars(Set<TriplePattern> aPattern) {
		Set<TripleVar> allTVs = new HashSet<>();
		for (TriplePattern tp : aPattern) {
			for (Variable var : tp.getVars()) {
				allTVs.add(new TripleVar(tp, var));
			}
		}
		return allTVs;
	}

	public Set<Variable> getVars(Set<TriplePattern> aPattern) {
		Set<Variable> vars = new HashSet<Variable>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVars());
		}
		return vars;
	}

	private int getState() {
		return this.state;
	}

	public Set<Map<TriplePattern, TriplePattern>> invert(Set<Map<TriplePattern, TriplePattern>> set) {

		Set<Map<TriplePattern, TriplePattern>> inverted = new HashSet<>();
		for (Map<TriplePattern, TriplePattern> map : set) {
			Map<TriplePattern, TriplePattern> invertedMap = new HashMap<TriplePattern, TriplePattern>();
			for (Map.Entry<TriplePattern, TriplePattern> entry : map.entrySet()) {
				invertedMap.put(entry.getValue(), entry.getKey());
			}
			inverted.add(invertedMap);
		}
		return inverted;

	}

	/**
	 * This method should be used by the taskboard whenever a bindingset has been
	 * produced.
	 * 
	 * @param bs
	 */
	public void setBindingSet(BindingSet bs) {

		// make sure the combinedBindings does not contain partial bindings.

		this.resultingBindingSet = bs;

		this.state = BINDINGSET_AVAILABLE;
	}

	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabIndex) {
		StringBuilder sb = new StringBuilder();
		String stateText = getStateText(this.state);

		if (this.getState() == BINDINGSET_AVAILABLE) {
			sb.append(this.resultingBindingSet);
		} else {
			sb.append(this.rule.consequent).append(" <-");
			sb.append(stateText).append("- ");
			sb.append(this.rule.antecedent);
		}
		sb.append("\n");
		for (NodeAlt child : children.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(child.toString(tabIndex + 1));
		}
		return sb.toString();
	}

	private String getStateText(int state2) {

		if (state2 == BINDINGSET_NOT_REQUESTED)
			return "x";
		if (state2 == BINDINGSET_REQUESTED)
			return "=";
		if (state2 == BINDINGSET_AVAILABLE)
			return "-";
		return null;
	}

	public RuleAlt getRule() {
		return this.rule;
	}

}
