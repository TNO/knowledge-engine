package eu.knowledge.engine.reasonerprototype.api;

import java.util.HashMap;
import java.util.Map;

import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Literal;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

public class Binding extends HashMap<TriplePattern.Variable, TriplePattern.Literal> {

	private static final long serialVersionUID = 2381462612239850018L;

	public Binding() {
		super();
	}

	public Binding(TriplePattern.Variable var, TriplePattern.Literal lit) {
		super();
		this.put(var, lit);
	}

	public Binding(String var, String val) {
		this(new TriplePattern.Variable(var), new TriplePattern.Literal(val));
	}

	public Binding(Binding b) {
		super(b);
	}

	public Literal put(String var, String val) {
		return this.put(new TriplePattern.Variable(var), new TriplePattern.Literal(val));
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

	public Map<String, String> toMap() {

		Map<String, String> result = new HashMap<String, String>();
		for (Map.Entry<Variable, Literal> entry : this.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue().toString());
		}

		return result;
	}

	public void putMap(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}

	}

}
