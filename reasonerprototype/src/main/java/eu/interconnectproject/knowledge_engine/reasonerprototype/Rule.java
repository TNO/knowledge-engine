package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Value;

public abstract class Rule {

	private final List<TriplePattern> lhs;
	private final List<TriplePattern> rhs;

	public Rule(List<TriplePattern> lhs, List<TriplePattern> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public List<TriplePattern> getLhs() {
		return lhs;
	}

	public List<TriplePattern> getRhs() {
		return rhs;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [" + lhs + " -> " + rhs + "]";
	}

	public boolean rhsMatches(TriplePattern objective, Binding binding) {
		for (TriplePattern rhsTriple : rhs) {
			if (rhsTriple.matches(objective, binding)) {
				return true;
			}
		}
		return false;
	}

	public List<TriplePattern> rhsMatchesTriples(TriplePattern objective, Binding binding) {
		List<TriplePattern> result = new ArrayList<>();
		for (TriplePattern rhsTriple : rhs) {
			if (rhsTriple.matches(objective, binding)) {
				result.add(rhsTriple);
			}
		}
		return result;
	}

	public boolean rhsMatches(List<TriplePattern> objective, Binding binding) {
		// A subset of objective needs to match
		for (TriplePattern objectiveTriple : objective) {
			if (rhsMatches(objectiveTriple, binding)) {
				return true;
			}
		}
		return false;
	}

	public List<TriplePattern> rhsMatchesTriples(List<TriplePattern> objective, Binding binding) {
		List<TriplePattern> result = new ArrayList<>();
		for (TriplePattern objectiveTriple : objective) {
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
	public List<List<TriplePattern>> rhsMatchesAlt(List<TriplePattern> objective, List<TriplePattern> rhs) {
		List<List<TriplePattern>> isos = new ArrayList<>();

		TriplePattern objTriple = objective.get(0);
		Map<TriplePattern, Map<Value, Value>> allMatches = rhsMatchesAlt(objTriple, rhs);

		List<TriplePattern> reducedObj = new ArrayList<>(objective);
		reducedObj.remove(0);
		List<TriplePattern> reducedRhs;
		for (Map.Entry<TriplePattern, Map<Value, Value>> entry : allMatches.entrySet()) {

			reducedRhs = new ArrayList<>(rhs);
			// TODO assumes every triple only occurs once (which is not the case,
			// because we have a list).
			reducedRhs.remove(entry.getKey());

			if (!reducedObj.isEmpty()) {

				List<List<TriplePattern>> otherIsos = rhsMatchesAlt(reducedObj, reducedRhs);

				for (List<TriplePattern> iso : otherIsos) {
					iso.add(0, entry.getKey());
				}

				isos.addAll(otherIsos);
			} else {
				List<TriplePattern> otherIsos = new ArrayList<TriplePattern>();
				otherIsos.add(entry.getKey());
				isos.add(otherIsos);
			}
		}

		return isos;
	}

	public Map<TriplePattern, Map<Value, Value>> rhsMatchesAlt(TriplePattern objTriple, List<TriplePattern> rhs) {
		Map<TriplePattern, Map<Value, Value>> allMatches = new HashMap<>();
		for (TriplePattern myTriple : rhs) {
			Map<Value, Value> map = myTriple.matchesWithSubstitutionMap(objTriple);
			if (map != null)
				allMatches.put(myTriple, map);
		}
		return allMatches;
	}

}
