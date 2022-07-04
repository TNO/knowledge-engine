package eu.knowledge.engine.reasoner.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.sparql.core.Var;

public class BindingSet extends HashSet<Binding> {
	private static final long serialVersionUID = 8263643495419009027L;

	public BindingSet() {
		super();
	}

	public BindingSet(Collection<Binding> bindings) {
		super();
		this.addAll(bindings);
	}

	public BindingSet(Binding... bindings) {
		super();
		for (Binding binding : bindings) {
			this.add(binding);
		}
	}

	/**
	 * Extend the bindingset into a graph bindingset. It will contain the same
	 * amount of bindings, but variable keys are now accompanied by the Triple in
	 * which they occur.
	 * 
	 * @param aGraphPattern
	 * @return
	 */
	public TripleVarBindingSet toTripleVarBindingSet(Set<TriplePattern> aGraphPattern) {

		TripleVarBindingSet gbs = new TripleVarBindingSet(aGraphPattern);

		TripleVarBinding tvb;
		for (Binding b : this) {
			tvb = new TripleVarBinding();
			for (TriplePattern triplePattern : aGraphPattern) {
				for (Var variable : triplePattern.getVariables()) {
					if (b.containsKey(variable)) {
						tvb.put(new TripleVar(triplePattern, variable), b.get(variable));
					}
				}
			}
			gbs.add(tvb);
		}
		return gbs;
	}

	public void addAll(Set<Map<String, String>> maps) {

		BindingSet bs = new BindingSet();
		Binding b;
		for (Map<String, String> map : maps) {
			b = new Binding();
			b.putMap(map);
			bs.add(b);
		}
		this.addAll(bs);
	}

	/**
	 * Write this BindingSet to the standard output.
	 * This is convenient for debugging.
	 */
	public void write() {
		for (Binding b : this) {
			System.out.println(b);
		}

	}

}
