/**
 * 
 */
package eu.knowledge.engine.reasoner.rulestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.TriplePattern;

/**
 * The rule store contains all the rules that are to be considered by the
 * reasoner.
 * 
 * @author nouwtb
 *
 */
public class RuleStore {

	private static final Logger LOG = LoggerFactory.getLogger(RuleStore.class);

	/**
	 * All the rules in this store.
	 */
	private Map<Rule, RuleNode> ruleToRuleNode;

	/**
	 * Instantiate an empty rule store.
	 */
	public RuleStore() {
		ruleToRuleNode = new HashMap<>();
	}

	public void addRule(Rule aRule) {
		RuleNode aRuleNode = new RuleNode(aRule);
		this.ruleToRuleNode.put(aRule, aRuleNode);
	}

	/**
	 * @param someRules all the rules this store should contain.
	 */
	public void addRules(Set<Rule> someRules) {

		for (Rule r : someRules) {
			addRule(r);
		}
	}

	/**
	 * @return all the rules of this store.
	 */
	public Set<Rule> getRules() {

		return this.ruleToRuleNode.values().stream().map(rn -> rn.getRule()).collect(Collectors.toSet());
	}

	/**
	 * Calculate the antecedent neighbors of this rule. That means all the other
	 * rules in {@code store} whose consequent matches this rule's antecedent. Note
	 * that it also adds the same information to the neighbor.
	 * 
	 * @return A mapping from a neighbor rulenode and the way its consequent matches
	 *         this rule's antecedent.
	 */
	public Map<Rule, Set<Match>> getAntecedentNeighbors(Rule aRule) {

		RuleNode aRuleNode = this.ruleToRuleNode.get(aRule);

		for (Rule someRule : this.getRules()) {
			RuleNode someRuleNode = this.ruleToRuleNode.get(someRule);
			if (!someRule.getConsequent().isEmpty() && !aRuleNode.getAntecedentNeighbors().containsKey(someRule)) {
				Set<Match> someMatches = aRule.antecedentMatches(someRule.getConsequent(),
						MatchStrategy.FIND_ALL_MATCHES);
				if (!someMatches.isEmpty()) {
					aRuleNode.setAntecedentNeighbor(someRule, someMatches);
					someRuleNode.setConsequentNeighbor(aRule, Match.invert(someMatches));
				}

			}
		}
		return aRuleNode.getAntecedentNeighbors();
	}

	/**
	 * Calculate the consequent neighbors of this rule. That means all the other
	 * rules in {@code store} whose antecedent matches this rule's antecedent. Note
	 * that it also adds the same information to the neighbor.<br />
	 * 
	 * This method is cached to improve performance.
	 * 
	 * @return A mapping from a neighbor rulenode and the way its antecedent matches
	 *         this rule's consequent.
	 */
	public Map<Rule, Set<Match>> getConsequentNeighbors(Rule aRule) {
		RuleNode aRuleNode = this.ruleToRuleNode.get(aRule);

		assert aRuleNode != null;

		for (Rule someRule : this.getRules()) {
			RuleNode someRuleNode = this.ruleToRuleNode.get(someRule);
			if (!someRule.getAntecedent().isEmpty() && !aRuleNode.getConsequentNeighbors().containsKey(someRule)) {
				Set<Match> someMatches = aRule.consequentMatches(someRule.getAntecedent(),
						MatchStrategy.FIND_ALL_MATCHES);
				if (!someMatches.isEmpty()) {
					aRuleNode.setConsequentNeighbor(someRule, someMatches);
					someRuleNode.setAntecedentNeighbor(aRule, Match.invert(someMatches));
				}

			}
		}
		return aRuleNode.getConsequentNeighbors();

	}

	/**
	 * Resets all the rules in this store. I.e. removes the caching related to who's
	 * the neighbor of who.
	 */
	public void reset() {
		for (RuleNode r : this.ruleToRuleNode.values()) {
			r.reset();
		}
	}

	/**
	 * Prints all the rules and the connections between them in GraphViz encoding.
	 * Use code in: {@link http://magjac.com/graphviz-visual-editor/}
	 */
	public void printGraphVizCode() {

		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("digraph {").append("\n");
		Map<Rule, String> ruleToName = new HashMap<>();

		int ruleNumber = 1;

		for (RuleNode r : ruleToRuleNode.values()) {

			String currentName = ruleToName.get(r.getRule());
			if (currentName == null) {
				currentName = /* "rule" + ruleNumber; */ generateName(r.getRule());
				assert !currentName.isEmpty();
				ruleNumber++;
				String replaceAll = toStringRule(r.getRule()).replaceAll("\\\"", "\\\\\"");

				sb.append(currentName).append("[").append("tooltip=").append("\"").append(replaceAll).append("\"")
						.append("]").append("\n");
				ruleToName.put(r.getRule(), currentName);
			}

			Set<Rule> anteNeigh = this.getAntecedentNeighbors(r.getRule()).keySet();
			String neighName;
			for (Rule neighR : anteNeigh) {
				neighName = ruleToName.get(neighR);
				if (neighName == null) {
					neighName = /* "rule" + ruleNumber; */ generateName(neighR);
					assert !neighName.isEmpty();
					ruleNumber++;
					String replaceAll = toStringRule(neighR).replaceAll("\\\"", "\\\\\"");
					sb.append(neighName).append("[").append("tooltip=").append("\"").append(replaceAll).append("\"")
							.append("]").append("\n");
					ruleToName.put(neighR, neighName);
				}

				sb.append(neighName).append(Rule.ARROW).append(currentName).append("\n");

			}
		}

		sb.append("}");
		LOG.info(sb.toString());
	}

	private String toStringRule(Rule neighR) {
		return neighR.getAntecedent() + " -> " + neighR.getConsequent();
	}

	/**
	 * Generates a shorter name for a rule while still being reasonably
	 * recognizable.
	 * 
	 * @param r
	 * @return
	 */
	private String generateName(Rule r) {

		StringBuilder sb = new StringBuilder();

		if (!r.getAntecedent().isEmpty()) {
			for (TriplePattern tp : r.getAntecedent()) {
				sb.append(generateName(tp)).append(" ");
			}
		}

		sb.append("->\\n");

		if (!r.getConsequent().isEmpty()) {
			for (TriplePattern tp : r.getConsequent()) {
				sb.append(generateName(tp)).append(" ");
			}
			if (sb.length() > 0)
				sb.deleteCharAt(sb.length() - 1);
		}

		return "\"" + sb.toString() + "\"";
	}

	private String generateName(TriplePattern tp) {

		String name = Rule.EMPTY;
		if (tp.getPredicate().toString().contains("type") && !tp.getObject().isVariable())
			name = tp.getObject().toString();
		else {
			if (!tp.getSubject().isVariable())
				name = tp.getSubject().toString() + " ";
			if (!tp.getPredicate().isVariable())
				name += tp.getPredicate().toString() + " ";
			if (!tp.getObject().isVariable())
				name += tp.getObject().toString() + " ";
		}

		if (name.endsWith(" "))
			name = name.substring(0, name.length() - 1);

		if (name.isEmpty())
			name = "unknown";

		name = name.replaceAll("\\\"", "\\\\\"");

		return name;
	}

}
