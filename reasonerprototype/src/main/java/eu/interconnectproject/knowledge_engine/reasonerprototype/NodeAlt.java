package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public NodeAlt(Set<Triple> aGoal, List<RuleAlt> someRules, NodeAlt aParent, RuleAlt aRule) {
		this.goal = aGoal;
		this.allRules = someRules;
		this.parent = aParent;
		this.rule = aRule;
		this.children = new HashMap<NodeAlt, Set<Map<Triple, Triple>>>();

		if (this.goal != null) {

			// generate children
			Map<RuleAlt, Set<Map<Triple, Triple>>> relevantRules = findRulesWithOverlappingConsequences(goal);

			NodeAlt child;
			for (Map.Entry<RuleAlt, Set<Map<Triple, Triple>>> entry : relevantRules.entrySet()) {
				// create a childnode
				child = new NodeAlt(entry.getKey().antecedent, this.allRules, this, entry.getKey());
				this.children.put(child, entry.getValue());
			}

			// TODO we need some ininite loop detection (see transitivity rule)
		}
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

	/**
	 * Continues with the reasoning process. Each time this method is called, the
	 * reasoning process is try to continue. Note that this method expects the
	 * pending tasks to be executing between subsequent tasks to this method. This
	 * method will schedule additional tasks that need to be executed again before
	 * it can continue with the reasoning process.
	 * 
	 * @param bindingSet a complete bindingset when finished, otherwise null.
	 * @return
	 */
	public BindingSet continueReasoning(BindingSet bindingSet) {

		// send the binding set down the tree and combine the resulting bindingsets to
		// the actual result.

		if (this.state != BINDINGSET_AVAILABLE) {

			boolean allBindingSetsAvailable = true;
			int childState;
			for (NodeAlt child : this.children.keySet()) {
				Set<Map<Triple, Triple>> childMatch = this.children.get(child);

				childState = child.getState();

				BindingSet bs = child.continueReasoning(bindingSet.translate(childMatch));
				allBindingSetsAvailable &= (bs != null);
			}

			if (allBindingSetsAvailable) {

				BindingSet combinedBindings = new BindingSet(bindingSet);

				for (NodeAlt child : this.children.keySet()) {
					Set<Map<Triple, Triple>> childMatch = this.children.get(child);

					// we can combine all different matches of this child's consequent into a single
					// bindingset.
					BindingSet childBindings = child.continueReasoning(combinedBindings.translate(childMatch));
					BindingSet convertedChildBindings = childBindings.translate(childMatch);
					combinedBindings = combinedBindings.altMerge(convertedChildBindings);
				}

				if (rule != null) {
					Map<NodeAlt, BindingSet> tasks = TaskBoard.instance().tasks;
					BindingSet newBindingSet;
					if (tasks.containsKey(this)) {
						BindingSet bs = tasks.get(this);
						newBindingSet = bs.altMerge(combinedBindings);
						tasks.put(this, newBindingSet);
					} else {
						tasks.put(this, new BindingSet(combinedBindings));
					}
					this.state = BINDINGSET_REQUESTED;
				} else {
					this.resultingBindingSet = combinedBindings;
					this.state = BINDINGSET_AVAILABLE;
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
			return resultingBindingSet;
		}
	}

	private int getState() {
		return this.state;
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

	/**
	 * This method should be used by the taskboard whenever a bindingset has been
	 * produced.
	 * 
	 * @param bs
	 */
	public void setBindingSet(BindingSet bs) {
		this.resultingBindingSet = bs;
		this.state = BINDINGSET_AVAILABLE;
	}

	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabIndex) {
		StringBuilder sb = new StringBuilder();

		String stateText = getStateText(this.state);

		if (this.rule != null)
			sb.append(" <-").append(stateText).append("- ");
		sb.append(this.goal).append("\n");
		for (NodeAlt child : children.keySet()) {
			for (int i = 0; i < tabIndex + 1; i++)
				sb.append("\t");

			sb.append(child.rule != null ? child.rule.consequent : "").append(child.toString(tabIndex + 1));
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
