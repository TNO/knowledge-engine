package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Value;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Variable;

public class RuleAlt {

	public Set<Triple> antecedent;
	public Set<Triple> consequent;

	public BindingSetHandler bindingSetHandler;

	public RuleAlt(Set<Triple> anAntecedent, Set<Triple> aConsequent, BindingSetHandler aBindingSetHandler) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = aBindingSetHandler;
	}

	public RuleAlt(Set<Triple> anAntecedent, Set<Triple> aConsequent) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = new BindingSetHandler() {
			@Override
			public BindingSet handle(BindingSet bs) {

				BindingSet newBS = new BindingSet();

				Binding newB;

				Set<Variable> vars = RuleAlt.this.getVars(RuleAlt.this.consequent);
				for (Binding b : bs) {
					newB = new Binding();
					for (Variable v : vars) {
						if (b.containsKey(v)) {
							newB.put(v, b.get(v));
						} else {
							System.err.println(
									"Not all variable in the consequent are available in the antecedent of the rule. This type of rule should use a custom BindingHandler.");
						}
					}
					newBS.add(newB);
				}

				return newBS;
			}
		};
	}

	public Set<Variable> getVars(Set<Triple> aPattern) {
		Set<Variable> vars = new HashSet<Variable>();
		for (Triple t : aPattern) {
			vars.addAll(t.getVars());
		}
		return vars;
	}

	public Set<Map<Triple, Triple>> consequentMatches(Set<Triple> objective) {
		Set<Map<Triple, Triple>> matches = matches(objective, consequent, new HashMap<>());
		return matches;
	}

	public BindingSetHandler getBindingSetHandler() {
		return bindingSetHandler;
	}

	/**
	 * 
	 * We match every triple from the objective with every triple of the rhs.
	 * 
	 * TODO It's recursive and uses the Java stack. There is a limit to the stack
	 * size in Java, so this limits the sizes of the graph patterns. We consdered
	 * using a loop instead of recursing, but we could not figure out how to do it.
	 * Also, this method is far from efficient, we need to figure out a way to
	 * improve its performance. Maybe using the dynamic programming algorithm
	 * described via here: {@see <a href=
	 * "https://stackoverflow.com/questions/32163716/partial-string-matching-in-two-large-strings">https://stackoverflow.com/questions/32163716/partial-string-matching-in-two-large-strings</a>}.
	 * 
	 * @param objective
	 * @param binding
	 */
	private Set<Map<Triple, Triple>> matches(Set<Triple> objective, Set<Triple> consequent, Map<Value, Value> context) {
		Set<Map<Triple, Triple>> isos = new HashSet<>();

		for (Triple objTriple : objective) {
			Map<Triple, Map<Value, Value>> allMatches = rhsMatchesAlt(objTriple, consequent);

			Set<Triple> reducedObj = new HashSet<>(objective);
			reducedObj.remove(objTriple);
			Set<Triple> reducedRhs;
			for (Map.Entry<Triple, Map<Value, Value>> entry : allMatches.entrySet()) {

				reducedRhs = new HashSet<>(consequent);
				reducedRhs.remove(entry.getKey());
				Map<Value, Value> newContext = entry.getValue();
				Map<Value, Value> mergedContext;

				if ((mergedContext = mergeContexts(context, newContext)) != null) {

					Set<Map<Triple, Triple>> otherIsos = new HashSet<>();
					if (!reducedObj.isEmpty()) {

						otherIsos = matches(reducedObj, reducedRhs, mergedContext);

						if (!otherIsos.isEmpty()) {
							for (Map<Triple, Triple> iso : otherIsos) {
								iso.put(objTriple, entry.getKey());
							}
						} else {
							Map<Triple, Triple> otherIso = new HashMap<Triple, Triple>();
							otherIso.put(objTriple, entry.getKey());
							otherIsos.add(otherIso);
						}
					} else {
						Map<Triple, Triple> otherIso = new HashMap<Triple, Triple>();
						otherIso.put(objTriple, entry.getKey());
						otherIsos.add(otherIso);
					}
					isos.addAll(otherIsos);
				}
			}
		}
		return isos;
	}

	/**
	 * Returns null if the contexts have conflicting values.
	 * 
	 * @param existingContext
	 * @param newContext
	 * @return
	 */
	private Map<Value, Value> mergeContexts(Map<Value, Value> existingContext, Map<Value, Value> newContext) {

		Map<Value, Value> mergedContext = new HashMap<Value, Value>(existingContext);

		for (Map.Entry<Value, Value> newEntry : newContext.entrySet()) {
			if (existingContext.containsKey(newEntry.getKey())) {
				if (!existingContext.get(newEntry.getKey()).equals(newEntry.getValue())) {
					return null;
				}
			} else {
				mergedContext.put(newEntry.getKey(), newEntry.getValue());
			}
		}

		return mergedContext;
	}

	private Map<Triple, Map<Value, Value>> rhsMatchesAlt(Triple objTriple, Set<Triple> rhs) {
		Map<Triple, Map<Value, Value>> allMatches = new HashMap<>();
		for (Triple myTriple : rhs) {
			Map<Value, Value> map = myTriple.matchesWithSubstitutionMap(objTriple);
			if (map != null)
				allMatches.put(myTriple, map);
		}
		return allMatches;
	}

	@Override
	public String toString() {
		return "RuleAlt [antecedent=" + antecedent + ", consequent=" + consequent + "]";
	}

}
