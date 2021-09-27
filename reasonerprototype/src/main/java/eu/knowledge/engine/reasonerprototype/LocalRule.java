package eu.knowledge.engine.reasonerprototype;

import java.util.List;

import eu.knowledge.engine.reasonerprototype.api.TriplePattern;

public class LocalRule extends Rule {

	public LocalRule(List<TriplePattern> lhs, List<TriplePattern> rhs) {
		super(lhs, rhs);
	}

}
