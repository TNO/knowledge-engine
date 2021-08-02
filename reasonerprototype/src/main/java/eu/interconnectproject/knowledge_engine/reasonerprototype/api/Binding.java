package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.HashMap;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Variable;

public class Binding extends HashMap<Triple.Variable, Triple.Literal> {

	private static final long serialVersionUID = 2381462612239850018L;

	public boolean isOverlappingAndNotConflicting(Binding other) {
		int overlapCnt = 0;
		for (Entry<Variable, Literal> e : this.entrySet()) {
			if (other.containsKey(e.getKey())) {
				overlapCnt++;
				if (!e.getValue().equals(other.get(e.getKey()))) {
					return true;
				}
			}
		}
		return overlapCnt > 0;
	}

	public Binding merge(Binding other) {
		Binding b = new Binding();
		b.putAll(this);
		b.putAll(other);
		return b;
	}

}
