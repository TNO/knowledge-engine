package eu.knowledge.engine.reasoner;

import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class ProactiveRule extends BaseRule {

	public ProactiveRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		super(anAntecedent, aConsequent);

		if (!(anAntecedent.isEmpty() || aConsequent.isEmpty())) {
			throw new IllegalArgumentException("A proactive rule should have either antecedent or consequent empty.");
		}
	}
	
	public ProactiveRule(String aName, Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent)
	{
		this(anAntecedent, aConsequent);
		this.setName(aName);
	}

}
