package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;
import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

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

}
