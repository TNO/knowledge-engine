package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Value;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Variable;

public class RemoteRuleReasoningNode extends RuleReasoningNode {

	private final KeReasoner keReasoner;
	private final RemoteRule rule;
	private final Triple objective;
	private final Binding objectiveBinding;
	private Map<Value, Value> bindingKeyMap;
	private Binding ruleBinding;
	private final SingleObjectiveReasoningNode parent;
	private MultiObjectiveReasoningNode child;
	private BindingSet childBindingSet;

	public RemoteRuleReasoningNode(SingleObjectiveReasoningNode parent, KeReasoner keReasoner, RemoteRule rule,
			Triple objective, Binding objectiveBinding) {
		this.parent = parent;
		this.keReasoner = keReasoner;
		this.rule = rule;
		this.objective = objective;
		this.objectiveBinding = objectiveBinding;
	}

	public boolean plan() {
		// create mapping from rule binding to objective binding
		bindingKeyMap = rule.getRhs().matchesWithSubstitutionMap(objective);
		Map<Value, Value> bindingKeyMapInversed = new HashMap<>();
		for (Entry<Value, Value> entry : bindingKeyMap.entrySet()) {
			bindingKeyMapInversed.put(entry.getValue(), entry.getKey());
		}
		// Get the binding with the variables form the rule
		ruleBinding = new Binding();
		for (Entry<Variable, Literal> e : objectiveBinding.entrySet()) {
			if (bindingKeyMapInversed.get(e.getKey()) instanceof Variable) {
				ruleBinding.put((Variable) bindingKeyMapInversed.get(e.getKey()), e.getValue());
			}
		}

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

	public BindingSet getKnowledgeInteractionBinding() {
		if (child == null) {
			return new BindingSet(ruleBinding);
		} else {
			return childBindingSet;
		}
	}

	public void processKnowledgeInteractionResponse(BindingSet bindingSet) {
		// Translate the binding back
		List<Binding> objectiveBindings = new ArrayList<>();
		for (Binding binding : bindingSet) {
			Binding objectiveResultBinding = new Binding();
			for (Entry<Variable, Literal> e : binding.entrySet()) {
				if (bindingKeyMap.containsKey(e.getKey())) {
					// Only provide bindings that were requested
					objectiveResultBinding.put((Variable) bindingKeyMap.get(e.getKey()), e.getValue());
				}
			}
			objectiveResultBinding.putAll(objectiveBinding);
			objectiveBindings.add(objectiveResultBinding);
		}
		BindingSet objectiveBindingSet = new BindingSet(objectiveBindings);
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
