package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Value;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Variable;

public class RuleAlt {

	public Set<TriplePattern> antecedent;
	public Set<TriplePattern> consequent;

	public BindingSetHandler bindingSetHandler;

	public RuleAlt(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent,
			BindingSetHandler aBindingSetHandler) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = aBindingSetHandler;
	}

	public RuleAlt(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
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

	public Set<Variable> getVars(Set<TriplePattern> aPattern) {
		Set<Variable> vars = new HashSet<Variable>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVars());
		}
		return vars;
	}

	public Set<Map<TriplePattern, TriplePattern>> consequentMatches(Set<TriplePattern> objective) {
		Set<Map<TriplePattern, TriplePattern>> matches = matches(objective, consequent, new HashMap<>());
		return matches;
	}

	public Set<Map<TriplePattern, TriplePattern>> consequentMatches2(Set<TriplePattern> objective) {
		Set<Map<TriplePattern, TriplePattern>> matches = new HashSet<>();

		for (TriplePattern objTriple : objective) {
			for (TriplePattern consTriple : this.consequent) {

			}

		}

		return matches;
	}

	public BindingSetHandler getBindingSetHandler() {
		return bindingSetHandler;
	}

	/**
	 * 
	 * We match every triple from the objective with every triple of the consequent.
	 * 
	 * TODO It's recursive and uses the Java stack. There is a limit to the stack
	 * size in Java, so this limits the sizes of the graph patterns. We considered
	 * using a loop instead of recursing, but we could not figure out how to do it.
	 * Also, this method is far from efficient, we need to figure out a way to
	 * improve its performance. Maybe using the dynamic programming algorithm
	 * described via here: {@see <a href=
	 * "https://stackoverflow.com/questions/32163716/partial-string-matching-in-two-large-strings">https://stackoverflow.com/questions/32163716/partial-string-matching-in-two-large-strings</a>}.
	 * 
	 * @param objective
	 * @param binding
	 */
	private Set<Map<TriplePattern, TriplePattern>> matches(Set<TriplePattern> objective, Set<TriplePattern> consequent,
			Map<Value, Value> context) {
		Set<Map<TriplePattern, TriplePattern>> isos = new HashSet<>();

		for (TriplePattern objTriple : objective) {
			Map<TriplePattern, Map<Value, Value>> allMatches = rhsMatchesAlt(objTriple, consequent);

			Set<TriplePattern> reducedObj = new HashSet<>(objective);
			reducedObj.remove(objTriple);
			Set<TriplePattern> reducedRhs;
			for (Map.Entry<TriplePattern, Map<Value, Value>> entry : allMatches.entrySet()) {

				// add singleton match
				Map<TriplePattern, TriplePattern> singletonMatch = new HashMap<TriplePattern, TriplePattern>();
				singletonMatch.put(objTriple, entry.getKey());
				isos.add(singletonMatch);

				reducedRhs = new HashSet<>(consequent);
				reducedRhs.remove(entry.getKey());
				Map<Value, Value> newContext = entry.getValue();
				Map<Value, Value> mergedContext;

				if ((mergedContext = mergeContexts(context, newContext)) != null) {

					Set<Map<TriplePattern, TriplePattern>> otherIsos = new HashSet<>();
					if (!reducedObj.isEmpty()) {

						otherIsos = matches(reducedObj, reducedRhs, mergedContext);

						if (!otherIsos.isEmpty()) {
							for (Map<TriplePattern, TriplePattern> iso : otherIsos) {
								iso.put(objTriple, entry.getKey());
							}
						} else {
							Map<TriplePattern, TriplePattern> otherIso = new HashMap<TriplePattern, TriplePattern>();
							otherIso.put(objTriple, entry.getKey());
							otherIsos.add(otherIso);
						}
					} else {
						Map<TriplePattern, TriplePattern> otherIso = new HashMap<TriplePattern, TriplePattern>();
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

	private Map<TriplePattern, Map<Value, Value>> rhsMatchesAlt(TriplePattern objTriple, Set<TriplePattern> rhs) {
		Map<TriplePattern, Map<Value, Value>> allMatches = new HashMap<>();
		for (TriplePattern myTriple : rhs) {
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

	public Set<Variable> getVars() {

		Set<Variable> vars = new HashSet<>();
		;
		if (this.antecedent != null)
			vars.addAll(this.getVars(this.antecedent));

		if (this.consequent != null)
			vars.addAll(this.getVars(this.consequent));

		return vars;
	}

}
