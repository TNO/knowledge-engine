package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;

public class LocalRule extends Rule {

	public LocalRule(List<TriplePattern> lhs, List<TriplePattern> rhs) {
		super(lhs, rhs);
	}

}
