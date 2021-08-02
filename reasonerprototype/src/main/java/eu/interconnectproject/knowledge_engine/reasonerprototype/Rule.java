package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public abstract class Rule {

	private final List<Triple> lhs;
	private final Triple rhs;

	public Rule(List<Triple> lhs, Triple rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public List<Triple> getLhs() {
		return lhs;
	}

	public Triple getRhs() {
		return rhs;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [" + lhs + " -> " + rhs + "]";
	}

	public boolean rhsMatches(Triple objective, Binding binding) {
		return rhs.matches(objective, binding);
	}

}
