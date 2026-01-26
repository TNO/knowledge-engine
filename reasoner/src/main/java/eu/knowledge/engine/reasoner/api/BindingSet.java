package eu.knowledge.engine.reasoner.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class BindingSet extends HashSet<Binding> {
	private static final long serialVersionUID = 8263643495419009027L;

	public BindingSet() {
		super();
	}

	public BindingSet(Collection<Binding> bindings) {
		super();
		this.addAll(bindings);
	}

	public BindingSet(ResultSet rs) {
		QuerySolution qs = null;
		while (rs.hasNext()) {
			qs = rs.next();
			this.add(new Binding(qs));
		}
	}

	public BindingSet(Binding... bindings) {
		super();
		for (Binding binding : bindings) {
			this.add(binding);
		}
	}

	public static BindingSet fromStringData(Collection<Map<String, String>> bindings) {
		BindingSet bs = new BindingSet();
		bindings.forEach(b -> bs.add(new Binding(b)));
		return bs;
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
			for (TriplePattern tp : aGraphPattern) {
				if (tp.getSubject().isVariable() && b.containsKey(tp.getSubject())) {
					tvb.put(new TripleNode(tp, tp.getSubject(), 0), b.get(tp.getSubject()));
				}
				if (tp.getPredicate().isVariable() && b.containsKey(tp.getPredicate())) {
					tvb.put(new TripleNode(tp, tp.getPredicate(), 1), b.get(tp.getPredicate()));
				}
				if (tp.getObject().isVariable() && b.containsKey(tp.getObject())) {
					tvb.put(new TripleNode(tp, tp.getObject(), 2), b.get(tp.getObject()));
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
	 * Write this BindingSet to the standard output. This is convenient for
	 * debugging.
	 */
	public void write() {
		for (Binding b : this) {
			System.out.println(b);
		}

	}

}
