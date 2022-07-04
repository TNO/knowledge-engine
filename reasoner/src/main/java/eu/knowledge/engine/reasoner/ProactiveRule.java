package eu.knowledge.engine.reasoner;

import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class ProactiveRule extends Rule {

	public ProactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		super(anAntecedent, aConsequent);
	}

}
