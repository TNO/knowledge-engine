package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.HashMap;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Variable;

public class Binding extends HashMap<Triple.Variable, Triple.Literal> {

	private static final long serialVersionUID = 2381462612239850018L;

	public Binding() {
		super();
	}

	public Binding(Triple.Variable var, Triple.Literal lit) {
		super();
		this.put(var, lit);
	}

	public Binding(String var, String val) {
		this(new Triple.Variable(var), new Triple.Literal(val));
	}

	public Literal put(String var, String val) {
		return this.put(new Triple.Variable(var), new Triple.Literal(val));
	}

	/**
	 * True if two Bindings have a different value for at least one variable
	 */
	public boolean isConflicting(Binding other) {
		for (Entry<Variable, Literal> e : this.entrySet()) {
			if (other.containsKey(e.getKey())) {
				if (!e.getValue().equals(other.get(e.getKey()))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * True if two Bindings have the same value for at least one variable
	 */
	public boolean isOverlapping(Binding other) {
		for (Entry<Variable, Literal> e : this.entrySet()) {
			if (other.containsKey(e.getKey())) {
				if (e.getValue().equals(other.get(e.getKey()))) {
					return true;
				}
			}
		}
		return false;
	}

	public Binding merge(Binding other) {
		assert !this.isConflicting(other);

		Binding b = new Binding();
		b.putAll(this);
		b.putAll(other);
		return b;
	}

}
