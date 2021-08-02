package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class LocalRule extends Rule {

	public LocalRule(List<Triple> lhs, Triple rhs) {
		super(lhs, rhs);
	}

}
