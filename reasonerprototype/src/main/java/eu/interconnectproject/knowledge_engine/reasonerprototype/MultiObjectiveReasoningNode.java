package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class MultiObjectiveReasoningNode implements ReasoningNode {

	private final ReasoningNode parent;
	private final KeReasoner keReasoner;
	private final List<Triple> objectives;
	private final Binding binding;

	private final ChildDataBase<SingleObjectiveReasoningNode> children = new ChildDataBase<>();

	public MultiObjectiveReasoningNode(ReasoningNode parent, KeReasoner keReasoner, List<Triple> objectives,
			Binding binding) {
		this.parent = parent;
		this.keReasoner = keReasoner;
		this.objectives = objectives;
		this.binding = binding;
	}

	public boolean plan() {
		System.out.println("Planning objective: " + objectives + " with binding " + binding);
		// Find rules
		for (Triple objective : objectives) {
			SingleObjectiveReasoningNode childNode = new SingleObjectiveReasoningNode(this, keReasoner, objective,
					binding);
			children.add(childNode);
			if (!childNode.plan()) {
				// All objectives need to be satisfied in order to succeed
				return false;
			}
		}
		return true;
	}

	@Override
	public void processResultingBindingSet(ReasoningNode child, BindingSet bs) {
		children.getChildData((SingleObjectiveReasoningNode) child).setResult(bs);
		if (children.allChildrenFinished()) {
			parent.processResultingBindingSet(this, children.getMergedBindingSet());
		}
	}

}
