package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.Map;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TriplePattern.Literal;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;

public class Binding extends HashMap<TriplePattern.Variable, TriplePattern.Literal> {

	private static final long serialVersionUID = 2381462612239850018L;

	public Binding() {
		super();
	}

	public Binding(TriplePattern.Variable variable, TriplePattern.Literal lit) {
		super();
		this.put(variable, lit);
	}

	public Binding(String variable, String val) {
		this(new TriplePattern.Variable(variable), new TriplePattern.Literal(val));
	}

	public Binding(Binding b) {
		super(b);
	}

	public boolean containsKey(String variable) {
		return this.containsKey(new Variable(variable));
	}

	public Literal get(String variable) {
		return this.get(new Variable(variable));
	}

	public Literal put(String variable, String val) {
		return this.put(new TriplePattern.Variable(variable), new TriplePattern.Literal(val));
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
