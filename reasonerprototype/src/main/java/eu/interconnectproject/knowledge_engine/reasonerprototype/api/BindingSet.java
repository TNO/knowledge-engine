package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.ArrayList;
import java.util.Collection;

public class BindingSet extends ArrayList<Binding> {
	private static final long serialVersionUID = 8263643495419009027L;

	public BindingSet() {
		super();
	}

	public BindingSet(Collection<Binding> bindings) {
		super();
		this.addAll(bindings);
	}

	public BindingSet(Binding binding) {
		super();
		this.add(binding);
	}

	public BindingSet merge(BindingSet other) {
		if (this.isEmpty()) {
			return new BindingSet(other);
		}
		BindingSet merged = new BindingSet();
		for (Binding bThis : this) {
			for (Binding bThat : other) {
				if (bThis.isOverlappingAndNotConflicting(bThat)) {
					merged.add(bThis.merge(bThat));
				}
			}
		}
		return merged;
	}

}
