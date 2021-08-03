package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.HashMap;
import java.util.Map;

public class Triple {

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

	public Triple(Value subject, Value predicate, Value object) {
		// TODO I assume a variable name is used only once
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public Triple(String string) {
		Value subject, predicate, object;
		String[] split = string.split(" ");
		if (split[0].startsWith("?")) {
			subject = new Triple.Variable(split[0]);
		} else {
			subject = new Triple.Literal(split[0]);
		}
		if (split[1].startsWith("?")) {
			predicate = new Triple.Variable(split[1]);
		} else {
			predicate = new Triple.Literal(split[1]);
		}
		if (split[2].startsWith("?")) {
			object = new Triple.Variable(split[2]);
		} else {
			object = new Triple.Literal(split[2]);
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

	public Triple substitude(Binding b) {
		Value subject, predicate, object;
		if (this.subject instanceof Variable && b.containsKey((this.subject))) {
			subject = b.get(this.subject);
		} else {
			subject = this.subject;
		}
		if (this.predicate instanceof Variable && b.containsKey((this.predicate))) {
			predicate = b.get(this.predicate);
		} else {
			predicate = this.predicate;
		}
		if (this.object instanceof Variable && b.containsKey((this.object))) {
			object = b.get(this.object);
		} else {
			object = this.object;
		}
		return new Triple(subject, predicate, object);
	}

	public boolean matches(Triple other, Binding binding) {
		Triple substituted = other.substitude(binding);
		return this.matches(substituted);
	}

	public boolean matches(Triple other) {
		if (this.getSubject() instanceof Literal && other.getSubject() instanceof Literal
				&& !this.getSubject().equals(other.subject)) {
			return false;
		}
		if (this.getPredicate() instanceof Literal && other.getPredicate() instanceof Literal
				&& !this.getPredicate().equals(other.getPredicate())) {
			return false;
		}
		if (this.getObject() instanceof Literal && other.getObject() instanceof Literal
				&& !this.getObject().equals(other.getObject())) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if two Triples match, and when they do, return a map of how values map
	 * between the two triples. This map is used to translatae a binding with the
	 * variable names from one triple the variable names of the other.
	 *
	 * @param other
	 * @return
	 */
	public Map<Triple.Value, Triple.Value> matchesWithSubstitutionMap(Triple other) {
		Map<Triple.Value, Triple.Value> substitutionMap = new HashMap<>();
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
		return subject + " " + predicate + " " + object + " . ";
	}

	public boolean containsVariables() {
		return subject instanceof Variable || predicate instanceof Variable || object instanceof Variable;
	}

}
