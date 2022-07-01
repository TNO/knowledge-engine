package eu.knowledge.engine.reasoner;

import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class ProactiveRule extends Rule {

	public ProactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent,
			BindingSetHandler aBindingSetHandler) {
		super(anAntecedent, aConsequent, aBindingSetHandler);

		if (!anAntecedent.isEmpty() && aConsequent.isEmpty()) {
			throw new IllegalArgumentException(
					"A proactive rule should have either an empty antecedent or consequent.");
		}
	}

	public ProactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		super(anAntecedent, aConsequent);
	}

}
