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

	public TripleVarBinding() {
		tripleVarMapping = new HashMap<>();
	}

	public TripleVarBinding(Set<TriplePattern> aGraphPattern, Binding aBinding) {
		this();
		for (TriplePattern tp : aGraphPattern) {
			for (Variable var : tp.getVariables()) {
				if (aBinding.containsKey(var))
					tripleVarMapping.put(new TripleVar(tp, var), aBinding.get(var));
			}
		}
	}

	public TripleVarBinding(TripleVarBinding b) {
		this();
		for (Map.Entry<TripleVar, Literal> entry : b.tripleVarMapping.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
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
	}

	public void put(TripleVar aTripleVar, String aLiteral) {
		tripleVarMapping.put(aTripleVar, new Literal(aLiteral));
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
			b.put(entry.getKey().var, entry.getValue());
		}
		return b;
	}

	/**
	 * True if two Bindings have a different value for at least one variable. Note
	 * that it looks not at variable instances.
	 */
	public boolean isConflicting(TripleVarBinding tvb) {

		for (Map.Entry<TripleVar, Literal> e : this.tripleVarMapping.entrySet()) {
			Literal l = tvb.getVarValue(e.getKey().var);

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
	private Literal getVarValue(Variable var) {
		for (TripleVar tripleVar : this.tripleVarMapping.keySet()) {
			if (tripleVar.var.equals(var)) {
				return this.get(tripleVar);
			}
		}
		return null;
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
			sb.append(prefix).append(entry.getKey().var).append("=").append(entry.getValue());
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
