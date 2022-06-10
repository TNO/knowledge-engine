/**
 * 
 */
package eu.knowledge.engine.reasoner;

import java.util.HashSet;
import java.util.Set;

/**
 * The rule store contains all the rules that are to be considered by the
 * reasoner.
 * 
 * @author nouwtb
 *
 */
public class RuleStore {

	/**
	 * All the rules in this store.
	 */
	private Set<Rule> rules;

	/**
	 * Instantiate an empty rule store.
	 */
	public RuleStore() {
		rules = new HashSet<Rule>();
	}

	public void addRule(Rule aRule) {

		if (aRule.getStore().equals(this))
			throw new IllegalArgumentException("The rule should have this store as its store.");

		this.rules.add(aRule);
	}

	/**
	 * @param someRules all the rules this store should contain.
	 */
	public void addRules(Set<Rule> someRules) {

		for (Rule r : someRules) {
			this.rules.add(r);
		}
	}

	/**
	 * @return all the rules of this store.
	 */
	public Set<Rule> getRules() {
		return this.rules;
	}
}
