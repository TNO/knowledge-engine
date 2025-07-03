package eu.knowledge.engine.reasoner.rulestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
import eu.knowledge.engine.reasoner.BaseRule.MatchFlag;
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
	 * rule's antecedent according to the configured {@link MatchFlag}
	 */
	private Map<BaseRule, Set<Match>> antecedentNeighbors;

	/**
	 * The combi matches to antecedent neighbors.
	 * 
	 * These might not get initialized, because we cannot initialize these
	 * symmetrically like the antencedentNeighbors mapping can be initialized.
	 * 
	 * If this set remains null, it is not applicable. If it is empty, it is
	 * delibarately initalized with an empty list, because there are no combi
	 * matches on the antecedent side of this node.If it is non-empty, there are
	 * combi matches to one or more nodes on the consequent side of this node.
	 */
	private Set<CombiMatch> antecedentCombiMatches;

	/**
	 * All other rules in the {@link MatchNode#store} whose antecedents match this
	 * rule's consequent according to the {@link MatchFlag}
	 */
	private Map<BaseRule, Set<Match>> consequentNeighbors;

	/**
	 * The combi matches to consequent neighbors.
	 * 
	 * Thesemightnot get initialize,because we cannot initialize these symmetrically
	 * like the consequentNeighbors mapping can be initialized.
	 * 
	 * If this set remains null, it is not applicable. If it is empty, it is
	 * delibarately initalized with an empty list, because there are no combi
	 * matches on the consequent side of this node. If it is non-empty, there are
	 * combi matches to one or more nodes on the consequent side of this node.
	 * 
	 */
	private Set<CombiMatch> consequentCombiMatches;

	public MatchNode(BaseRule aRule) {
		this.rule = aRule;
		this.antecedentNeighbors = new HashMap<>();
		this.consequentNeighbors = new HashMap<>();
	}

	public BaseRule getRule() {
		return this.rule;
	}

	public void setConsequentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		Map<BaseRule, Set<Match>> neighbors = this.consequentNeighbors;

		if (!neighbors.containsKey(aRule)) {
			neighbors.put(aRule, someMatches);
		}
	}

	public void setAntecedentNeighbor(BaseRule aRule, Set<Match> someMatches) {
		Map<BaseRule, Set<Match>> neighbors = this.antecedentNeighbors;

		if (!neighbors.containsKey(aRule)) {
			neighbors.put(aRule, someMatches);
		}
	}

	/**
	 * Set the combi matches for the consequent side of this node.
	 * 
	 * @param someCombiMatches The combi matches for this node on the consequent
	 *                         side.
	 */
	public void setConsequentMatches(Set<CombiMatch> someCombiMatches) {
		this.consequentCombiMatches = someCombiMatches;
	}

	/**
	 * Set the combi matches for the antecedent side of this node.
	 * 
	 * @param someCombiMatches The combi matches for this node on the antecedent
	 *                         side.
	 */
	public void setAntecedentCombiMatches(Set<CombiMatch> someCombiMatches) {
		this.antecedentCombiMatches = someCombiMatches;
	}

	public Set<CombiMatch> getConsequentCombiMatches() {
		return this.consequentCombiMatches;
	}

	public Set<CombiMatch> getAntecedentCombiMatches() {
		return this.antecedentCombiMatches;
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
		this.antecedentCombiMatches.clear();
		this.antecedentNeighbors.clear();
		this.consequentCombiMatches.clear();
		this.consequentNeighbors.clear();

	}
}
