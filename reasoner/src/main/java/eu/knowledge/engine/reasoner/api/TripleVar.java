package eu.knowledge.engine.reasoner.api;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVar;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;

public class TripleVar {
	public TriplePattern tp;
	public Variable variable;

	public TripleVar(TriplePattern aTriplePattern, Variable aVariable) {
		this.tp = aTriplePattern;
		this.variable = aVariable;
	}

	public TripleVar(TriplePattern aTriplePattern, String aVariable) {
		this(aTriplePattern, new Variable(aVariable));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tp == null) ? 0 : tp.hashCode());
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TripleVar)) {
			return false;
		}
		TripleVar other = (TripleVar) obj;
		if (tp == null) {
			if (other.tp != null) {
				return false;
			}
		} else if (!tp.equals(other.tp)) {
			return false;
		}
		if (variable == null) {
			if (other.variable != null) {
				return false;
			}
		} else if (!variable.equals(other.variable)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "TripleVar [tp=" + tp + ", var=" + variable + "]";
	}
}