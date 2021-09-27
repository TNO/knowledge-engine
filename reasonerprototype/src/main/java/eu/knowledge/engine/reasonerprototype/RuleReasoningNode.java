package eu.knowledge.engine.reasonerprototype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Literal;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Value;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

public abstract class RuleReasoningNode<R extends Rule> implements ReasoningNode {

	protected final MultiObjectiveReasoningNode parent;
	protected final KeReasoner keReasoner;
	/** Reasoning objective for this rule */
	protected final List<TriplePattern> objective;
	/** Binding for the objective with variable names from objective */
	protected final Binding objectiveBinding;

	/**
	 * Map between variable names of the rule to the variable names of the objective
	 */
	protected Map<Value, Value> bindingKeyMap;
	/** Binding for the objective with variable names from the rule */
	protected Binding ruleBinding;
	/** Child node. Only used when rule has a lhs */
	protected MultiObjectiveReasoningNode child;
	/** BindingSet received from child (if there is one) */
	protected BindingSet childBindingSet;
	protected final R rule;

	public RuleReasoningNode(MultiObjectiveReasoningNode parent, KeReasoner keReasoner, List<TriplePattern> objective,
			Binding objectiveBinding, R rule) {
		this.parent = parent;
		this.keReasoner = keReasoner;
		this.objective = objective;
		this.objectiveBinding = objectiveBinding;
		this.rule = rule;
	}

	public R getRule() {
		return rule;
	}

	/**
	 * Example:
	 *
	 * Objective: ?sens rdf:type Sensor
	 *
	 * Objective binding {?sens=sensor1}
	 *
	 * bindingKeyMap {?SENSOR=?sens
	 *
	 * bindingKeyMapInverted {?sens=?SENSOR}
	 *
	 * Rule: [] -> [?SENSOR rdf:type Sensor]
	 *
	 * Rule binding: {?SENSOR=sensor1}
	 */

	protected void createRuleBinding() {
		Map<Value, Value> bindingKeyMapInverted = new HashMap<>();
		for (Entry<Value, Value> entry : bindingKeyMap.entrySet()) {
			bindingKeyMapInverted.put(entry.getValue(), entry.getKey());
		}
		// Get the binding with the variables from the rule
		ruleBinding = new Binding();
		for (Entry<Variable, Literal> e : objectiveBinding.entrySet()) {
			if (bindingKeyMapInverted.get(e.getKey()) instanceof Variable) {
				ruleBinding.put((Variable) bindingKeyMapInverted.get(e.getKey()), e.getValue());
			}
		}
	}

	protected void createBindingKeyMap() {
		bindingKeyMap = new HashMap<>();
		for (TriplePattern rhsTriple : rule.getRhs()) {
			for (TriplePattern objectiveTriple : objective) {
				Map<Value, Value> subMap = rhsTriple.matchesWithSubstitutionMap(objectiveTriple);
				if (subMap != null) {
					bindingKeyMap.putAll(subMap);
				}
			}
		}
	}

	protected BindingSet translateResultingBindingSet(BindingSet bindingSet) {
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
		return objectiveBindingSet;
	}

}
