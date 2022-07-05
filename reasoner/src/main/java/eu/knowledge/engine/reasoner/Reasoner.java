package eu.knowledge.engine.reasoner;

import java.util.Collection;
import java.util.Set;

import eu.knowledge.engine.reasoner.rulestore.RuleStore;

/**
 * TODO Do we need to class?
 * 
 * @author nouwtb
 *
 */
public class Reasoner {

	private RuleStore store;

	public Reasoner() {
		store = new RuleStore();
	}

	public void addRule(Rule aRule) {
		this.store.addRule(aRule);
	}

	public void addRules(Set<Rule> someRules) {
		this.store.addRules(someRules);
	}

	public void removeRule(Rule aRule) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public void removeRules(Collection<Rule> someRules) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public ReasonerPlan plan(ProactiveRule theStartRule) {
		return new ReasonerPlan(this.store, theStartRule);
	}
}