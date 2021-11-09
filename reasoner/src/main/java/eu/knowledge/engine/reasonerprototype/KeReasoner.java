package eu.knowledge.engine.reasonerprototype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.knowledge.engine.reasonerprototype.Rule.MatchStrategy;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;

public class KeReasoner {

	// rules might need an order to prevent infinite loops
	private List<Rule> rules = new ArrayList<Rule>();

	public void addRule(Rule rule) {
		rules.add(rule);
	}

	public ReasoningNode backwardPlan(Set<TriplePattern> aGoal, MatchStrategy aMatchStrategy, TaskBoard aTaskboard) {
		Rule goalRule = new Rule(aGoal, new HashSet<>(), new BindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public BindingSet handle(BindingSet bs) {
				return bs;
			}

		});
		ReasoningNode root = new ReasoningNode(rules, null, goalRule, aMatchStrategy, true, aTaskboard);
		return root;
	}

	public ReasoningNode forwardPlan(Set<TriplePattern> aPremise, MatchStrategy aMatchStrategy, TaskBoard aTaskboard) {

		Rule premiseRule = new Rule(new HashSet<>(), aPremise, new BindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public BindingSet handle(BindingSet bs) {
				return bs;
			}
		});

		ReasoningNode root = new ReasoningNode(rules, null, premiseRule, aMatchStrategy, false, aTaskboard);

		return root;
	}

	public List<Rule> getRules() {
		return this.rules;
	}

}
