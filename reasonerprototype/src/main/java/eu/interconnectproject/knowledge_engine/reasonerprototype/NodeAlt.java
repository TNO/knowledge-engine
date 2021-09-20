package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class NodeAlt {

	/**
	 * Null for root
	 */
	private NodeAlt parent;
	/**
	 * Empty list if no more goals.
	 */
	private Set<Triple> goal;

	private Map<NodeAlt, Set<Map<Triple, Triple>>> children;

	private List<RuleAlt> allRules;
	private RuleAlt rule;

	public NodeAlt(Set<Triple> aGoal, List<RuleAlt> someRules, NodeAlt aParent, RuleAlt aRule) {
		this.goal = aGoal;
		this.allRules = someRules;
		this.parent = aParent;
		this.rule = aRule;
		this.children = new HashMap<NodeAlt, Set<Map<Triple, Triple>>>();

		// generate children
		Map<RuleAlt, Set<Map<Triple, Triple>>> relevantRules = findRulesWithOverlappingConsequences(goal);

		NodeAlt child;
		for (Map.Entry<RuleAlt, Set<Map<Triple, Triple>>> entry : relevantRules.entrySet()) {
			// create a childnode
			child = new NodeAlt(entry.getKey().antecedent, this.allRules, this, entry.getKey());
			this.children.put(child, entry.getValue());
		}

		// we need some ininite loop detection (see transitivity rule)
	}

	private Map<RuleAlt, Set<Map<Triple, Triple>>> findRulesWithOverlappingConsequences(Set<Triple> aGoal) {

		Set<Map<Triple, Triple>> possibleMatches;
		Map<RuleAlt, Set<Map<Triple, Triple>>> overlappingRules = new HashMap<>();
		for (RuleAlt r : this.allRules) {

			if (!(possibleMatches = r.consequentMatches(aGoal)).isEmpty()) {
				overlappingRules.put(r, possibleMatches);
			}
		}
		return overlappingRules;

	}

	public BindingSet reason(BindingSet bindingSet) {

		// send the binding set down the tree and combine the resulting bindingsets to
		// the actual result.

		// TODO internally the bindingset should contain bindings with variables where
		// instead of having a single entry per variable name, we need an entry per
		// variable occurrence. This is to make sure that different predicates going to
		// the same two variables are still correctly matched.
		// we start however, with the naive version that does not support this.
		BindingSet allBindings = new BindingSet(bindingSet);

		if (this.rule != null) {

			Map<RuleAlt, BindingSet> tasks = TaskBoard.instance().tasks;
			BindingSet newBindingSet;
			if (tasks.containsKey(this.rule)) {
				BindingSet bs = tasks.get(this.rule);
				newBindingSet = bs.altMerge(bindingSet);
			} else {
				tasks.put(this.rule, new BindingSet(bindingSet));
			}
			tasks.put(this.rule, bindingSet);
		}

		for (NodeAlt child : this.children.keySet()) {
			Set<Map<Triple, Triple>> childMatch = this.children.get(child);

			BindingSet childBindings = child.reason(allBindings.translate(childMatch));
			BindingSet convertedChildBindings = childBindings.translate(invert(childMatch));
			allBindings = allBindings.altMerge(convertedChildBindings);
		}

		// TODO remove partial bindings

		return allBindings;
	}

	public Set<Map<Triple, Triple>> invert(Set<Map<Triple, Triple>> set) {

		Set<Map<Triple, Triple>> inverted = new HashSet<>();
		for (Map<Triple, Triple> map : set) {

			Map<Triple, Triple> invertedMap = new HashMap<Triple, Triple>();
			for (Map.Entry<Triple, Triple> entry : map.entrySet()) {

				invertedMap.put(entry.getValue(), entry.getKey());

			}
			inverted.add(invertedMap);

		}
		return inverted;

	}

	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabIndex) {
		StringBuilder sb = new StringBuilder();

		if (this.rule != null)
			sb.append(" <- ");
		sb.append(this.goal).append("\n");
		for (NodeAlt child : children.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(child.rule != null ? child.rule.consequent : "").append(child.toString(tabIndex + 1));
		}
		return sb.toString();
	}

}
