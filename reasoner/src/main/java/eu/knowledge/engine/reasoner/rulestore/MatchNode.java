package eu.knowledge.engine.reasoner.rulestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.BaseRule;

/**
 * This class encapsulates and stores the information about which rule matches
 * what other rule. We do not store it in the actual {@code Rule} class to make
 * sure the {@code Rule} class can be reused and can remain independent of the
 * RuleStore.
 * 
 * @author nouwtb
 *
 */
public class MatchNode {

	private BaseRule rule;

	/**
	 * The store to which this rule belongs. A rule can only belong to a single
	 * store.
	 */
	private RuleStore store;

	/**
	 * All other rules in the {@link MatchNode#store} whose consequents match this
	 * rule's antecedent either fully or partially.
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighbors;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent either fully or partially.
	 */
	private Map<BaseRule, Set<Match>> consequentNeighbors;

	public MatchNode(BaseRule aRule) {
		this.rule = aRule;
		this.antecedentNeighbors = new HashMap<>();
		this.consequentNeighbors = new HashMap<>();
	}

	public BaseRule getRule() {
		return this.rule;
	}

	public void setConsequentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		if (!this.consequentNeighbors.containsKey(aRule)) {
			this.consequentNeighbors.put(aRule, someMatches);
		}
	}

	public void setAntecedentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		if (!this.antecedentNeighbors.containsKey(aRule)) {
			this.antecedentNeighbors.put(aRule, someMatches);
		}
	}

	public Map<BaseRule, Set<Match>> getConsequentNeighbors() {
		return this.consequentNeighbors;
	}

	public Map<BaseRule, Set<Match>> getAntecedentNeighbors() {
		return this.antecedentNeighbors;
	}

	/**
	 * @return The store this rule belongs to.
	 */
	public RuleStore getStore() {
		return this.store;
	}

	public void reset() {
		this.antecedentNeighbors.clear();
		this.consequentNeighbors.clear();

	}
}
