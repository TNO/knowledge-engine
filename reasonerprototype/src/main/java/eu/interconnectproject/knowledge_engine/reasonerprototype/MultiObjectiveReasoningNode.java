package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.ChildDataNodeBase.ChildData;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class MultiObjectiveReasoningNode implements ReasoningNode {

	private final ReasoningNode parent;
	private final KeReasoner keReasoner;
	private final List<Triple> objectives;
	private final Binding binding;

	private final ChildDataNodeBase<RuleReasoningNode<?>> children = new ChildDataNodeBase<>();

	public MultiObjectiveReasoningNode(ReasoningNode parent, KeReasoner keReasoner, List<Triple> objectives,
			Binding binding) {
		this.parent = parent;
		this.keReasoner = keReasoner;
		this.objectives = objectives;
		this.binding = binding;
	}

	@Override
	public boolean plan() {
		System.out.println("Planning objective: " + objectives + " with binding " + binding);
		// TODO I think we can do something more clever here in order to avoid
		// unnecessary requests to KI's
		List<Rule> rules = keReasoner.findRulesFor(objectives, binding);
		System.out.println("Found rules: " + rules);

		boolean success = false;
		for (Rule rule : rules) {
			RuleReasoningNode<?> childNode = null;
			if (rule instanceof RemoteRule) {
				childNode = new RemoteRuleReasoningNode(this, keReasoner, objectives, binding, (RemoteRule) rule);
			} else if (rule instanceof LocalRule) {
				childNode = new LocalRuleReasoningNode(this, keReasoner, objectives, binding, (LocalRule) rule);
			}
			// Only one rule has to match in order for this planning to be successful
			if (childNode.plan()) {
				children.add(childNode);
				success = true;
			}

			// All all objectives covered by these rules?
			for (Triple objective : objectives) {
				boolean found = false;
				for (ChildData<RuleReasoningNode<?>> cd : children) {
					if (cd.getChild().getRule().rhsMatches(objective, binding)) {
						found = true;
						break;
					}
				}
				if (!found) {
					// For this objective there was no match
					return false;
				}
			}
		}
		return success;
	}

	@Override
	public void processResultingBindingSet(ReasoningNode child, BindingSet bs) {
		children.getChildData((RuleReasoningNode<?>) child).setResult(bs);
		if (children.allChildrenFinished()) {
			parent.processResultingBindingSet(this, children.getMergedBindingSet());
		}
	}

}
