package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVar;
import eu.knowledge.engine.reasoner.api.TripleVarBinding;
import eu.knowledge.engine.reasoner.api.TriplePattern.Literal;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;

/**
 * A binding where the keys are triple/variable pairs.
 * 
 * @author nouwtb
 *
 */
public class TripleVarBinding {

	private Map<TripleVar, Literal> tripleVarMapping;

	/**
	 * Optimize the getVarValue method.
	 */
	private Map<Variable, TripleVar> variableTripleVarMapping;

	public TripleVarBinding() {
		tripleVarMapping = new HashMap<>();
		variableTripleVarMapping = new HashMap<>();
	}

	public TripleVarBinding(Set<TriplePattern> aGraphPattern, Binding aBinding) {
		this();
		for (TriplePattern tp : aGraphPattern) {
			for (Variable variable : tp.getVariables()) {
				if (aBinding.containsKey(variable)) {
					TripleVar tripleVar = new TripleVar(tp, variable);
					tripleVarMapping.put(tripleVar, aBinding.get(variable));
					variableTripleVarMapping.put(variable, tripleVar);
				}
			}
		}
	}

	public TripleVarBinding(TripleVarBinding b) {
		this();
		for (Map.Entry<TripleVar, Literal> entry : b.tripleVarMapping.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<Variable, TripleVar> entry : b.variableTripleVarMapping.entrySet()) {
			this.variableTripleVarMapping.put(entry.getKey(), entry.getValue());
		}
	}

	public Set<Map.Entry<TripleVar, Literal>> entrySet() {
		return tripleVarMapping.entrySet();
	}

	public Set<TripleVar> getTripleVars() {
		return tripleVarMapping.keySet();
	}

	public void put(TripleVar aTripleVar, Literal aLiteral) {
		tripleVarMapping.put(aTripleVar, aLiteral);
		variableTripleVarMapping.put(aTripleVar.variable, aTripleVar);
	}

	public void put(TripleVar aTripleVar, String aLiteral) {
		tripleVarMapping.put(aTripleVar, new Literal(aLiteral));
		variableTripleVarMapping.put(aTripleVar.variable, aTripleVar);
	}

	/**
	 * Convert the TripleVarBinding to the Binding. I.e. collapse the TripleVars
	 * with the same Vars into Vars. Note that if a particular triplevar is
	 * undefined, but another triple with the same var is defined, it will show up
	 * in the resulting binding.
	 * 
	 * @return
	 */
	public Binding toBinding() {

		Binding b = new Binding();
		for (Map.Entry<TripleVar, Literal> entry : this.tripleVarMapping.entrySet()) {
			b.put(entry.getKey().variable, entry.getValue());
		}
		return b;
	}

	/**
	 * True if two Bindings have a different value for at least one variable. Note
	 * that it looks not at variable instances.
	 */
	public boolean isConflicting(TripleVarBinding tvb) {

		for (Map.Entry<TripleVar, Literal> e : this.tripleVarMapping.entrySet()) {
			Literal l = tvb.getVarValue(e.getKey().variable);

			if (l != null && !e.getValue().equals(l)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * We assume all occurrences of a var have the same literal, we just return the
	 * first one found.
	 * 
	 * @param var
	 * @return
	 */
	private Literal getVarValue(Variable variable) {
		TripleVar tripleVar = this.variableTripleVarMapping.get(variable);
		return this.get(tripleVar);
	}

	public Literal get(TripleVar key) {
		return this.tripleVarMapping.get(key);
	}

	public boolean containsKey(TripleVar key) {
		return this.tripleVarMapping.containsKey(key);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tripleVarMapping == null) ? 0 : tripleVarMapping.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TripleVarBinding)) {
			return false;
		}
		TripleVarBinding other = (TripleVarBinding) obj;
		if (tripleVarMapping == null) {
			if (other.tripleVarMapping != null) {
				return false;
			}
		} else if (!tripleVarMapping.equals(other.tripleVarMapping)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		String prefix = "";
		sb.append("{");
		for (Map.Entry<TripleVar, Literal> entry : this.tripleVarMapping.entrySet()) {
			sb.append(prefix).append(entry.getKey().variable).append("=").append(entry.getValue());
			prefix = ",";

		}
		sb.append("}");
		return sb.toString();
	}

	public Set<TripleVar> keySet() {
		return this.tripleVarMapping.keySet();
	}

	/**
	 * We assume these two triplevarbindings do not conflict (responsibility of the
	 * caller to check!) and return the merged triplevarbinding. Duplicates are
	 * automatically removed because triple var bindings are sets.
	 * 
	 * @param otherB
	 * @return
	 */
	public TripleVarBinding merge(TripleVarBinding otherB) {

		assert !this.isConflicting(otherB);
		TripleVarBinding b = new TripleVarBinding();
		b.putAll(this.tripleVarMapping);
		b.putAll(otherB.tripleVarMapping);
		return b;
	}

	private void putAll(Map<TripleVar, Literal> aTripleVarMapping) {

		for (TripleVar tv : aTripleVarMapping.keySet()) {
			this.variableTripleVarMapping.put(tv.variable, tv);
		}
		this.tripleVarMapping.putAll(aTripleVarMapping);
	}

	public boolean isEmpty() {
		return this.tripleVarMapping.isEmpty();
	}

	public boolean containsTriplePattern(TriplePattern value) {
		for (TripleVar tv : this.tripleVarMapping.keySet()) {
			if (tv.tp.equals(value)) {
				return true;
			}
		}
		return false;
	}

}
