package eu.knowledge.engine.reasonerprototype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;

public class KeReasonerAlt {

	// rules might need an order to prevent infinite loops
	private List<RuleAlt> rules = new ArrayList<RuleAlt>();

	public void addRule(RuleAlt rule) {
		rules.add(rule);
	}

	public NodeAlt plan(Set<TriplePattern> aGoal, boolean aFullMatchOnly) {
		RuleAlt goalRule = new RuleAlt(aGoal, new HashSet<>(), new BindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public BindingSet handle(BindingSet bs) {
				return bs;
			}

		});
		NodeAlt root = new NodeAlt(rules, null, goalRule, aFullMatchOnly);
		return root;
	}

}
