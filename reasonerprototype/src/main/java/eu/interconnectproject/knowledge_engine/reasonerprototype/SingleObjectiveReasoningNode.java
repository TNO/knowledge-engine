package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class SingleObjectiveReasoningNode implements ReasoningNode {

	private final KeReasoner keReasoner;
	private final Triple objective;
	private final Binding binding;
	private final MultiObjectiveReasoningNode parent;
	private final ChildDataBase<RuleReasoningNode> children = new ChildDataBase<>();

	public SingleObjectiveReasoningNode(MultiObjectiveReasoningNode parent, KeReasoner keReasoner, Triple objective,
			Binding binding) {
		this.parent = parent;
		this.keReasoner = keReasoner;
		this.objective = objective;
		this.binding = binding;
	}

	public boolean plan() {
		List<Rule> rules = keReasoner.findRulesFor(objective, binding);
		System.out.println("Found rules: " + rules);
		boolean success = false;
		for (Rule rule : rules) {
			RuleReasoningNode childNode = null;
			if (rule instanceof RemoteRule) {
				childNode = new RemoteRuleReasoningNode(this, keReasoner, (RemoteRule) rule, objective, binding);
			} else if (rule instanceof LocalRule) {
				childNode = new LocalRuleReasoningNode(this, keReasoner, (LocalRule) rule, objective, binding);
			}
			// Only one rule has to match in order for this planning to be successful
			if (childNode.plan()) {
				children.add(childNode);
				success = true;
			}
		}
		return success;
	}

	@Override
	public void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet) {
		children.getChildData((RemoteRuleReasoningNode) child).setResult(bindingSet);
		if (children.allChildrenFinished()) {
			parent.processResultingBindingSet(this, children.getMergedBindingSet());
		}
	}

}
