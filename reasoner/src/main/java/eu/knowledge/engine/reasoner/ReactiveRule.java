package eu.knowledge.engine.reasoner;

import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class ReactiveRule extends Rule {

	public ReactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent,
			BindingSetHandler aBindingSetHandler) {
		super(anAntecedent, aConsequent, aBindingSetHandler);
	}

	public ReactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		super(anAntecedent, aConsequent);
	}
}
