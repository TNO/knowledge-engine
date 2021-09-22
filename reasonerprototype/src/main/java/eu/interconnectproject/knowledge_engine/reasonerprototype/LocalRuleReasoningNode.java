package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;

public class LocalRuleReasoningNode extends RuleReasoningNode<LocalRule> {

	public LocalRuleReasoningNode(MultiObjectiveReasoningNode parent, KeReasoner keReasoner, List<TriplePattern> objective,
			Binding objectiveBinding, LocalRule rule) {
		super(parent, keReasoner, objective, objectiveBinding, rule);
	}

	@Override
	public boolean plan() {
		// create mapping from rule binding to objective binding
		createBindingKeyMap();
		createRuleBinding();

		if (rule.getLhs().isEmpty()) {
			System.out.println("Finished planning at rule " + rule + " with binding " + ruleBinding);
			return true;
		} else {
			System.out.println("Substituting objective \"" + objective + "\" with binding " + objectiveBinding
					+ " for new objectives \"" + rule.getLhs() + "\" with binding " + ruleBinding + "\n");

			child = new MultiObjectiveReasoningNode(this, keReasoner, rule.getLhs(), ruleBinding);
			return child.plan();
		}
	}

	@Override
	public void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet) {
		// This is called if this node has a child (which it has when
		// !rule.getLhs().isEmpty())
		assert this.child != null;

		BindingSet objectiveBindingSet = translateResultingBindingSet(bindingSet);
		System.out.println("Binding for objective \"" + objective + "\" translated to " + objectiveBindingSet);

		parent.processResultingBindingSet(this, objectiveBindingSet);
	}
}
