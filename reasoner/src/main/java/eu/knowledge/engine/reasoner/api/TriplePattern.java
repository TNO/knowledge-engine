package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class TriplePattern {

	public static abstract class Value {

	}

	public static class Variable extends Value {
		private final String variableName;

		public Variable(String variableName) {
			this.variableName = variableName;
		}

		public String getVariableName() {
			return variableName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return variableName;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Variable other = (Variable) obj;
			if (variableName == null) {
				if (other.variableName != null)
					return false;
			} else if (!variableName.equals(other.variableName))
				return false;
			return true;
		}
	}

	public static class Literal extends Value {
		private final String value;

		public Literal(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Literal other = (Literal) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

	}

	private final Value subject;
	private final Value predicate;
	private final Value object;

	public TriplePattern(Value subject, Value predicate, Value object) {
		// TODO I assume a variable name is used only once
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public TriplePattern(String string) {
		Value subject, predicate, object;
		String[] split = string.split(" ");
		if (split[0].startsWith("?")) {
			subject = new TriplePattern.Variable(split[0]);
		} else {
			subject = new TriplePattern.Literal(split[0]);
		}
		if (split[1].startsWith("?")) {
			predicate = new TriplePattern.Variable(split[1]);
		} else {
			predicate = new TriplePattern.Literal(split[1]);
		}
		if (split[2].startsWith("?")) {
			object = new TriplePattern.Variable(split[2]);
		} else {
			object = new TriplePattern.Literal(split[2]);
		}
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public Value getSubject() {
		return subject;
	}

	public Value getPredicate() {
		return predicate;
	}

	public Value getObject() {
		return object;
	}

	/**
	 * Checks if two Triples match, and when they do, return a map of how values map
	 * between the two triples. This map is used to translatae a binding with the
	 * variable names from one triple the variable names of the other.
	 *
	 * The mapping is null if the triple patterns conflict, empty if nothing needs
	 * to be mapped to translate a bindingset from one to the other and non-empty if
	 * something needs to happen to translate one thing to the other.
	 *
	 * @param other
	 * @return
	 */
	public Map<TriplePattern.Value, TriplePattern.Value> findMatches(TriplePattern other) {
		Map<TriplePattern.Value, TriplePattern.Value> substitutionMap = new HashMap<>();
		if (this.getSubject() instanceof Literal && other.getSubject() instanceof Literal) {
			if (!this.getSubject().equals(other.getSubject())) {
				return null;
			}
		} else {
			// at least one of those is a variable

			substitutionMap.put(this.getSubject(), other.getSubject());
		}
		if (this.getPredicate() instanceof Literal && other.getPredicate() instanceof Literal) {
			if (!this.getPredicate().equals(other.getPredicate())) {
				return null;
			}
		} else {
			// at least one of those is a variable
			substitutionMap.put(this.getPredicate(), other.getPredicate());
		}
		if (this.getObject() instanceof Literal && other.getObject() instanceof Literal) {
			if (!this.getObject().equals(other.getObject())) {
				return null;
			}
		} else {
			// at least one of those is a variable
			substitutionMap.put(this.getObject(), other.getObject());
		}
		return substitutionMap;
	}

	@Override
	public String toString() {
		return subject + " " + predicate + " " + object;
	}

	public Set<Variable> getVariables() {

		Set<Variable> vars = new HashSet<>();
		if (this.getSubject() instanceof Variable) {
			vars.add((Variable) this.getSubject());
		}
		if (this.getPredicate() instanceof Variable) {
			vars.add((Variable) this.getPredicate());
		}
		if (this.getObject() instanceof Variable) {
			vars.add((Variable) this.getObject());
		}

		return vars;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TriplePattern)) {
			return false;
		}
		TriplePattern other = (TriplePattern) obj;
		if (object == null) {
			if (other.object != null) {
				return false;
			}
		} else if (!object.equals(other.object)) {
			return false;
		}
		if (predicate == null) {
			if (other.predicate != null) {
				return false;
			}
		} else if (!predicate.equals(other.predicate)) {
			return false;
		}
		if (subject == null) {
			if (other.subject != null) {
				return false;
			}
		} else if (!subject.equals(other.subject)) {
			return false;
		}
		return true;
	}

}
