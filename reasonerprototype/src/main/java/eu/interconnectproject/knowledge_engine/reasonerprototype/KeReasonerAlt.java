package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class KeReasonerAlt {

	// rules might need an order to prevent infinite loops
	private List<RuleAlt> rules = new ArrayList<RuleAlt>();

	public void addRule(RuleAlt rule) {
		rules.add(rule);
	}

	public NodeAlt plan(Set<Triple> aGoal) {
		NodeAlt root = new NodeAlt(aGoal, rules, null, null);
		return root;
	}

	public BindingSet reason(NodeAlt root, BindingSet aBindingSet) {
		return null;
	}

}
