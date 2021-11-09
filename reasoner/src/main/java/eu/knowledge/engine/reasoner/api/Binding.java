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

	public boolean containsKey(String var) {
		return this.containsKey(new Variable(var));
	}

	public Literal get(String var) {
		return this.get(new Variable(var));
	}

	public Literal put(String var, String val) {
		return this.put(new TriplePattern.Variable(var), new TriplePattern.Literal(val));
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
