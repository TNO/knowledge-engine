package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Value;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Variable;

public class BindingSet extends HashSet<Binding> {
	private static final long serialVersionUID = 8263643495419009027L;

	public BindingSet() {
		super();
	}

	public BindingSet(Collection<Binding> bindings) {
		super();
		this.addAll(bindings);
	}

	public BindingSet(Binding... bindings) {
		super();
		for (Binding binding : bindings) {
			this.add(binding);
		}
	}

	public BindingSet merge(BindingSet other) {
		// TODO Han and Wilco concluded that this merge algorithm isn't (always) correct
		if (this.isEmpty()) {
			return new BindingSet(other);
		}
		BindingSet merged = new BindingSet();
		BindingSet bs1 = new BindingSet(this);
		BindingSet bs2 = new BindingSet(other);

		Iterator<Binding> it1 = bs1.iterator();
		Iterator<Binding> it2 = bs2.iterator();

		outer: while (it1.hasNext()) {
			Binding b1 = it1.next();
			while (it2.hasNext()) {
				Binding b2 = it2.next();
				if (b1.isOverlapping(b2) && !b1.isConflicting(b2)) {
					merged.add(b1.merge(b2));
					it1.remove();
					it2.remove();
					continue outer;
				}
			}
		}

		merged.addAll(bs1);
		merged.addAll(bs2);

		return merged;
	}

	/**
	 * The current {@link BindingSet#merge(BindingSet)} method, does indeed not
	 * handle BindingTest#testStillOther() correctly.
	 * 
	 * The algorithm should be similar to a SQL join. If two bindings do not have
	 * overlapping variables, then each of the pairs of the one, should be combined
	 * with each of the pairs of the other.
	 * 
	 * If two bindings _do_ have overlapping variables, then it depends on whether
	 * all these overlapping variables have the same value or not what the action
	 * should be. If all overlapping variables have the same value, the two bindings
	 * can be merged into a single one. If not all overlapping variables have the
	 * same value, then both should be incorporated into the merged version.
	 * 
	 * Sometimes we want to remove bindings from the other bindingset that are
	 * incompatible with the existing bindings in this bindingset. The boolean
	 * incompatibleBindingsAllowed sets this behaviour.
	 * 
	 * @param other
	 * @return
	 */
	public BindingSet altMerge(BindingSet other, boolean incompatibleBindingsAllowed, boolean addCartesianProduct) {
		BindingSet merged = new BindingSet();

		if (this.isEmpty()) {
			for (Binding otherB : other) {
				merged.add(otherB);
			}
		} else {
			// Cartesian product is the base case
			for (Binding thisB : this) {
				merged.add(thisB);

				for (Binding otherB : other) {

					// always add a merged version of the two bindings, except when they conflict.
					if (!thisB.isConflicting(otherB)) {
						merged.add(otherB);
						if (addCartesianProduct) {
							merged.add(thisB.merge(otherB));
						}
					} else if (incompatibleBindingsAllowed) {
						merged.add(otherB);
					}
				}
			}
		}

		return merged;
	}

	/**
	 * Translate this bindingset using the given match. The variablenames will be
	 * changed and variables not relevant in the match will be removed.
	 * 
	 * @param match
	 * @return
	 */
	public BindingSet translate(Set<Map<TriplePattern, TriplePattern>> match) {

		BindingSet newOne = new BindingSet();
		Binding newB;
		for (Binding b : this) {
			newB = new Binding();
			for (Map.Entry<Variable, Literal> pair : b.entrySet()) {
				for (Map<TriplePattern, TriplePattern> entry : match) {
					Map<Value, Value> fullMap = convert(entry);
					if (pair.getKey() instanceof Variable) {
						Value v = fullMap.get(pair.getKey());
						if (v instanceof Variable) {
							Variable var = (Variable) v;
							newB.put(var, pair.getValue());
						}
					}
				}
			}
			newOne.add(newB);
		}
		return newOne;
	}

	public Map<Value, Value> invert(Map<Value, Value> incoming) {
		Map<Value, Value> outgoing = new HashMap<>();
		for (Map.Entry<Value, Value> entry : incoming.entrySet()) {
			outgoing.put(entry.getValue(), entry.getKey());
		}
		return outgoing;
	}

	public Map<Value, Value> convert(Map<TriplePattern, TriplePattern> match) {

		Map<Value, Value> fullMap = new HashMap<Value, Value>();

		for (Map.Entry<TriplePattern, TriplePattern> mapping : match.entrySet()) {
			Map<Value, Value> substitutionMap = mapping.getKey().matchesWithSubstitutionMap(mapping.getValue());
			fullMap.putAll(substitutionMap);
		}

		return fullMap;
	}

	/**
	 * Extend the bindingset into a graph bindingset. It will contain the same
	 * amount of bindings, but variable keys are now accompanied by the Triple in
	 * which they occur.
	 * 
	 * @param aGraphPattern
	 * @return
	 */
	public GraphBindingSet toGraphBindingSet(Set<TriplePattern> aGraphPattern) {

		GraphBindingSet gbs = new GraphBindingSet(aGraphPattern);

		TripleVarBinding tvb;
		for (Binding b : this) {
			tvb = new TripleVarBinding();
			for (TriplePattern triplePattern : aGraphPattern) {
				for (Variable var : triplePattern.getVars()) {
					if (b.containsKey(var)) {
						tvb.put(new TripleVar(triplePattern, var), b.get(var));
					}
				}
			}
			gbs.add(tvb);
		}
		return gbs;
	}

	public void addAll(Set<Map<String, String>> maps) {

		Binding b = new Binding();
		for (Map<String, String> map : maps) {
			b.putMap(map);
		}
		this.add(b);
	}

}
