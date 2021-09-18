package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Value;

public abstract class Rule {

	private final List<Triple> lhs;
	private final List<Triple> rhs;

	public Rule(List<Triple> lhs, List<Triple> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public List<Triple> getLhs() {
		return lhs;
	}

	public List<Triple> getRhs() {
		return rhs;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [" + lhs + " -> " + rhs + "]";
	}

	public boolean rhsMatches(Triple objective, Binding binding) {
		for (Triple rhsTriple : rhs) {
			if (rhsTriple.matches(objective, binding)) {
				return true;
			}
		}
		return false;
	}

	public List<Triple> rhsMatchesTriples(Triple objective, Binding binding) {
		List<Triple> result = new ArrayList<>();
		for (Triple rhsTriple : rhs) {
			if (rhsTriple.matches(objective, binding)) {
				result.add(rhsTriple);
			}
		}
		return result;
	}

	public boolean rhsMatches(List<Triple> objective, Binding binding) {
		// A subset of objective needs to match
		for (Triple objectiveTriple : objective) {
			if (rhsMatches(objectiveTriple, binding)) {
				return true;
			}
		}
		return false;
	}

	public List<Triple> rhsMatchesTriples(List<Triple> objective, Binding binding) {
		List<Triple> result = new ArrayList<>();
		for (Triple objectiveTriple : objective) {
			result.addAll(rhsMatchesTriples(objectiveTriple, binding));
		}
		return result;
	}

	/**
	 * 
	 * We match every triple from the objective with every triple of the rhs.
	 * 
	 * TODO It's recursive and uses the Java stack. There is a limit to the stack
	 * size in Java, so this limits the sizes of the graph patterns. We consdered
	 * using a loop instead of recursing, but we could not figure out how to do it.
	 * 
	 * @param objective
	 * @param binding
	 */
	public List<List<Triple>> rhsMatchesAlt(List<Triple> objective, List<Triple> rhs) {
		List<List<Triple>> isos = new ArrayList<>();

		Triple objTriple = objective.get(0);
		Map<Triple, Map<Value, Value>> allMatches = rhsMatchesAlt(objTriple, rhs);

		List<Triple> reducedObj = new ArrayList<>(objective);
		reducedObj.remove(0);
		List<Triple> reducedRhs;
		for (Map.Entry<Triple, Map<Value, Value>> entry : allMatches.entrySet()) {

			reducedRhs = new ArrayList<>(rhs);
			// TODO assumes every triple only occurs once (which is not the case,
			// because we have a list).
			reducedRhs.remove(entry.getKey());

			if (!reducedObj.isEmpty()) {

				List<List<Triple>> otherIsos = rhsMatchesAlt(reducedObj, reducedRhs);

				for (List<Triple> iso : otherIsos) {
					iso.add(0, entry.getKey());
				}

				isos.addAll(otherIsos);
			} else {
				List<Triple> otherIsos = new ArrayList<Triple>();
				otherIsos.add(entry.getKey());
				isos.add(otherIsos);
			}
		}

		return isos;
	}

	public Map<Triple, Map<Value, Value>> rhsMatchesAlt(Triple objTriple, List<Triple> rhs) {
		Map<Triple, Map<Value, Value>> allMatches = new HashMap<>();
		for (Triple myTriple : rhs) {
			Map<Value, Value> map = myTriple.matchesWithSubstitutionMap(objTriple);
			if (map != null)
				allMatches.put(myTriple, map);
		}
		return allMatches;
	}

}
