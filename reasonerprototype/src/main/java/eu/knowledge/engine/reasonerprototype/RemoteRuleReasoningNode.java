package eu.knowledge.engine.reasonerprototype;

import java.net.URI;
import java.util.List;

import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;

public class RemoteRuleReasoningNode extends RuleReasoningNode<RemoteRule> {

	public RemoteRuleReasoningNode(MultiObjectiveReasoningNode parent, KeReasoner keReasoner, List<TriplePattern> objective,
			Binding objectiveBinding, RemoteRule rule) {
		super(parent, keReasoner, objective, objectiveBinding, rule);
	}

	@Override
	public boolean plan() {
		// create mapping from rule binding to objective binding
		createBindingKeyMap();
		createRuleBinding();

		if (rule.getLhs().isEmpty()) {
			System.out.println("Finished planning at rule " + rule + " with binding " + ruleBinding);
			keReasoner.getRemoteTaskBoard().addRemoteTask(this);
			return true;
		} else {
			System.out.println("Substituting objective \"" + objective + "\" with binding " + objectiveBinding
					+ " for new objectives \"" + rule.getLhs() + "\" with binding " + ruleBinding + "\n");

			child = new MultiObjectiveReasoningNode(this, keReasoner, rule.getLhs(), ruleBinding);
			return child.plan();
		}
	}

	public URI getKnowledgeInteractionId() {
		return rule.getKnowledgeInteractionId();
	}

	public BindingSet getKnowledgeInteractionRequestBinding() {
		if (child == null) {
			// no lhs in rule, we can use the rule binding
			return new BindingSet(ruleBinding);
		} else {
			// lhs in rule, so this node has a child node
			return childBindingSet;
		}
	}

	public void processKnowledgeInteractionResponse(BindingSet bindingSet) {
		BindingSet objectiveBindingSet = translateResultingBindingSet(bindingSet);
		System.out.println("Binding for objective \"" + objective + "\" translated to " + objectiveBindingSet);

		parent.processResultingBindingSet(this, objectiveBindingSet);
	}

	@Override
	public void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet) {
		// This is called if this node has a child (which it has when
		// !rule.getLhs().isEmpty())
		assert this.child != null;

		this.childBindingSet = bindingSet;
		// We're now ready for contacting the KI
		keReasoner.getRemoteTaskBoard().addRemoteTask(this);
	}

}
