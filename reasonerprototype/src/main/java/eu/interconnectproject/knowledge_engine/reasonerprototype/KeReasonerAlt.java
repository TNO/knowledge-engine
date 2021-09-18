package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class KeReasonerAlt {

	// rules might need an order to prevent infinite loops
	private List<Rule> rules = new ArrayList<Rule>();

	public BindingSet reason(Set<Triple> goal, BindingSet bindings) {

		AltNode root = new AltNode(goal);

		Stack<AltNode> s = new Stack<>();
		s.push(root);

		while (!s.isEmpty()) {

			AltNode goalNode = s.pop();

		}
		return null;
	}

	private List<Rule> findRulesWithOverlappingConsequences(Set<Triple> aGoal, BindingSet bindings) {

		List<Rule> overlappingRules = new ArrayList<>();
		for (Rule r : this.rules) {

//			if (r.gpMatches(aGoal, r.getRhs())) {
//				overlappingRules.add(r);
//			}

		}
		return null;

	}

}
