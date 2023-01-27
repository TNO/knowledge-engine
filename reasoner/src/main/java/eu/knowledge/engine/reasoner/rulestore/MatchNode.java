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
	 * rule's antecedent according to the
	 * {@link MatchStrategy#FIND_ONLY_FULL_MATCHES}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighborsFull;

	/**
	 * All other rules in the {@link MatchNode#store} whose consequents match this
	 * rule's antecedent according to the
	 * {@link MatchStrategy#FIND_ONLY_BIGGEST_MATCHES}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighborsBiggest;

	/**
	 * All other rules in the {@link MatchNode#store} whose consequents match this
	 * rule's antecedent according to the {@link MatchStrategy#FIND_ALL_MATCHES}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighborsAll;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the
	 * {@link MatchStrategy#FIND_ONLY_FULL_MATCHES}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighborsFull;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the
	 * {@link MatchStrategy#FIND_ONLY_BIGGEST_MATCHES}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighborsBiggest;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the {@link MatchStrategy#FIND_ALL_MATCHES}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighborsAll;

	public MatchNode(BaseRule aRule) {
		this.rule = aRule;
		this.antecedentNeighborsFull = new HashMap<>();
		this.antecedentNeighborsBiggest = new HashMap<>();
		this.antecedentNeighborsAll = new HashMap<>();
		this.consequentNeighborsFull = new HashMap<>();
		this.consequentNeighborsBiggest = new HashMap<>();
		this.consequentNeighborsAll = new HashMap<>();
	}

	public BaseRule getRule() {
		return this.rule;
	}

	public void setConsequentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		this.setConsequentNeighbor(aRule, someMatches, MatchStrategy.FIND_ALL_MATCHES);
	}

	public void setAntecedentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		this.setAntecedentNeighbor(aRule, someMatches, MatchStrategy.FIND_ALL_MATCHES);
	}

	public void setConsequentNeighbor(BaseRule aRule, Set<Match> someMatches, MatchStrategy aStrategy) {
		Map<BaseRule, Set<Match>> neighbors;
		if (aStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
			neighbors = this.consequentNeighborsFull;
		} else if (aStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES)) {
			neighbors = this.consequentNeighborsBiggest;
		} else if (aStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)) {
			neighbors = this.consequentNeighborsAll;
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
		if (aStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
			neighbors = this.antecedentNeighborsFull;
		} else if (aStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES)) {
			neighbors = this.antecedentNeighborsBiggest;
		} else if (aStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)) {
			neighbors = this.antecedentNeighborsAll;
		} else {
			assert false;
			neighbors = new HashMap<>();
		}

		if (!neighbors.containsKey(aRule)) {
			neighbors.put(aRule, someMatches);
		}
	}

	public Map<BaseRule, Set<Match>> getConsequentNeighbors() {
		return this.getConsequentNeighbors(MatchStrategy.FIND_ALL_MATCHES);
	}

	public Map<BaseRule, Set<Match>> getAntecedentNeighbors() {
		return this.getAntecedentNeighbors(MatchStrategy.FIND_ALL_MATCHES);
	}

	public Map<BaseRule, Set<Match>> getConsequentNeighbors(MatchStrategy aStrategy) {
		if (aStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
			return this.consequentNeighborsFull;
		} else if (aStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES)) {
			return this.consequentNeighborsBiggest;
		} else if (aStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)) {
			return this.consequentNeighborsAll;
		} else {
			assert false;
			return null;
		}
	}

	public Map<BaseRule, Set<Match>> getAntecedentNeighbors(MatchStrategy aStrategy) {
		if (aStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
			return this.antecedentNeighborsFull;
		} else if (aStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES)) {
			return this.antecedentNeighborsBiggest;
		} else if (aStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)) {
			return this.antecedentNeighborsAll;
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
		this.antecedentNeighborsFull.clear();
		this.antecedentNeighborsBiggest.clear();
		this.antecedentNeighborsAll.clear();
		this.consequentNeighborsFull.clear();
		this.consequentNeighborsBiggest.clear();
		this.consequentNeighborsAll.clear();

	}
}
