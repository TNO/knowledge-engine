package eu.knowledge.engine.reasoner.rulestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.Match;

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
	 * rule's antecedent according to the {@link MatchStrategy#NORMAL_LEVEL}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighborsNormal;

	/**
	 * All other rules in the {@link MatchNode#store} whose consequents match this
	 * rule's antecedent according to the {@link MatchStrategy#ADVANCED_LEVEL}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighborsAdvanced;

	/**
	 * All other rules in the {@link MatchNode#store} whose consequents match this
	 * rule's antecedent according to the {@link MatchStrategy#ULTRA_LEVEL}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighborsUltra;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the {@link MatchStrategy#NORMAL_LEVEL}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighborsNormal;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the {@link MatchStrategy#ADVANCED_LEVEL}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighborsAdvanced;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the {@link MatchStrategy#ULTRA_LEVEL}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighborsUltra;

	public MatchNode(BaseRule aRule) {
		this.rule = aRule;
		this.antecedentNeighborsNormal = new HashMap<>();
		this.antecedentNeighborsAdvanced = new HashMap<>();
		this.antecedentNeighborsUltra = new HashMap<>();
		this.consequentNeighborsNormal = new HashMap<>();
		this.consequentNeighborsAdvanced = new HashMap<>();
		this.consequentNeighborsUltra = new HashMap<>();
	}

	public BaseRule getRule() {
		return this.rule;
	}

	public void setConsequentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		this.setConsequentNeighbor(aRule, someMatches, MatchStrategy.ULTRA_LEVEL);
	}

	public void setAntecedentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		this.setAntecedentNeighbor(aRule, someMatches, MatchStrategy.ULTRA_LEVEL);
	}

	public void setConsequentNeighbor(BaseRule aRule, Set<Match> someMatches, MatchStrategy aStrategy) {
		Map<BaseRule, Set<Match>> neighbors;
		if (aStrategy.equals(MatchStrategy.NORMAL_LEVEL) || aStrategy.equals(MatchStrategy.ENTRY_LEVEL)) {
			neighbors = this.consequentNeighborsNormal;
		} else if (aStrategy.equals(MatchStrategy.ADVANCED_LEVEL)) {
			neighbors = this.consequentNeighborsAdvanced;
		} else if (aStrategy.equals(MatchStrategy.ULTRA_LEVEL)) {
			neighbors = this.consequentNeighborsUltra;
		} else {
			assert false;
			neighbors = new HashMap<>();
		}

		if (!neighbors.containsKey(aRule)) {
			neighbors.put(aRule, someMatches);
		}
	}

	public void setAntecedentNeighbor(BaseRule aRule, Set<Match> someMatches, MatchStrategy aStrategy) {
		Map<BaseRule, Set<Match>> neighbors;
		if (aStrategy.equals(MatchStrategy.NORMAL_LEVEL) || aStrategy.equals(MatchStrategy.ENTRY_LEVEL)) {
			neighbors = this.antecedentNeighborsNormal;
		} else if (aStrategy.equals(MatchStrategy.ADVANCED_LEVEL)) {
			neighbors = this.antecedentNeighborsAdvanced;
		} else if (aStrategy.equals(MatchStrategy.ULTRA_LEVEL)) {
			neighbors = this.antecedentNeighborsUltra;
		} else {
			assert false;
			neighbors = new HashMap<>();
		}

		if (!neighbors.containsKey(aRule)) {
			neighbors.put(aRule, someMatches);
		}
	}

	public Map<BaseRule, Set<Match>> getConsequentNeighbors() {
		return this.getConsequentNeighbors(MatchStrategy.ULTRA_LEVEL);
	}

	public Map<BaseRule, Set<Match>> getAntecedentNeighbors() {
		return this.getAntecedentNeighbors(MatchStrategy.ULTRA_LEVEL);
	}

	public Map<BaseRule, Set<Match>> getConsequentNeighbors(MatchStrategy aStrategy) {
		if (aStrategy.equals(MatchStrategy.NORMAL_LEVEL)) {
			return this.consequentNeighborsNormal;
		} else if (aStrategy.equals(MatchStrategy.ADVANCED_LEVEL)) {
			return this.consequentNeighborsAdvanced;
		} else if (aStrategy.equals(MatchStrategy.ULTRA_LEVEL)) {
			return this.consequentNeighborsUltra;
		} else {
			assert false;
			return null;
		}
	}

	public Map<BaseRule, Set<Match>> getAntecedentNeighbors(MatchStrategy aStrategy) {
		if (aStrategy.equals(MatchStrategy.NORMAL_LEVEL)) {
			return this.antecedentNeighborsNormal;
		} else if (aStrategy.equals(MatchStrategy.ADVANCED_LEVEL)) {
			return this.antecedentNeighborsAdvanced;
		} else if (aStrategy.equals(MatchStrategy.ULTRA_LEVEL)) {
			return this.antecedentNeighborsUltra;
		} else {
			assert false;
			return null;
		}
	}

	/**
	 * @return The store this rule belongs to.
	 */
	public RuleStore getStore() {
		return this.store;
	}

	public void reset() {
		this.antecedentNeighborsNormal.clear();
		this.antecedentNeighborsAdvanced.clear();
		this.antecedentNeighborsUltra.clear();
		this.consequentNeighborsNormal.clear();
		this.consequentNeighborsAdvanced.clear();
		this.consequentNeighborsUltra.clear();

	}
}
