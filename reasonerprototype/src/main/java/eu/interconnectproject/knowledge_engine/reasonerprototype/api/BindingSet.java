package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class BindingSet extends ArrayList<Binding> {
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

	public BindingSet merge(BindingSet other) {
		// TODO Han and Wilco concluded that this merge algorithm isn't (always) correct
		if (this.isEmpty()) {
			return new BindingSet(other);
		}
		BindingSet merged = new BindingSet();
		BindingSet bs1 = new BindingSet(this);
		BindingSet bs2 = new BindingSet(other);

		Iterator<Binding> it1 = bs1.iterator();
		Iterator<Binding> it2 = bs2.iterator();

		outer: while (it1.hasNext()) {
			Binding b1 = it1.next();
			while (it2.hasNext()) {
				Binding b2 = it2.next();
				if (b1.isOverlapping(b2) && !b1.isConflicting(b2)) {
					merged.add(b1.merge(b2));
					it1.remove();
					it2.remove();
					continue outer;
				}
			}
		}

		merged.addAll(bs1);
		merged.addAll(bs2);

		return merged;
	}

	/**
	 * The current {@link BindingSet#merge(BindingSet)} method, does indeed not
	 * handle BindingTest#testStillOther() correctly.
	 * 
	 * The algorithm should be similar to a SQL join. If two bindings do not have
	 * overlapping variables, then each of the pairs of the one, should be combined
	 * with each of the pairs of the other.
	 * 
	 * If two bindings _do_ have overlapping variables, then it depends on whether
	 * all these overlapping variables have the same value or not what the action
	 * should be. If all overlapping variables have the same value, the two bindings
	 * can be merged into a single one. If not all overlapping variables have the
	 * same value, then both should be incorporated into the merged version.
	 * 
	 * @param other
	 * @return
	 */
	public BindingSet altMerge(BindingSet other) {
		BindingSet merged = new BindingSet();

		// Cartesian product is the base case
		for (Binding thisB : this) {
			for (Binding otherB : other) {

				// always add a merged version of the two bindings, except when they conflict.
				if (!thisB.isConflicting(otherB)) {
					merged.add(thisB.merge(otherB));
				}
			}
		}

		return merged;
	}

}
