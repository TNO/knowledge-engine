/**
 * 
 */
package eu.knowledge.engine.reasoner.rulestore;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.BaseRule.MatchFlag;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;

/**
 * The rule store contains all the rules that are to be considered by the
 * reasoner.
 * 
 * @author nouwtb
 *
 */
public class RuleStore {

	private static final int MAX_STR_LENGTH = 50;

	private static final Logger LOG = LoggerFactory.getLogger(RuleStore.class);

	/**
	 * All the rules in this store.
	 */
	private Map<BaseRule, MatchNode> ruleToRuleNode;

	/**
	 * Instantiate an empty rule store.
	 */
	public RuleStore() {
		ruleToRuleNode = new HashMap<>();
	}

	public void addRule(BaseRule aRule) {
		MatchNode aRuleNode = new MatchNode(aRule);
		this.ruleToRuleNode.put(aRule, aRuleNode);
	}

	/**
	 * @param someRules all the rules this store should contain.
	 */
	public void addRules(Set<BaseRule> someRules) {

		for (BaseRule r : someRules) {
			addRule(r);
		}
	}

	/**
	 * @return all the rules of this store.
	 */
	public Set<BaseRule> getRules() {

		return this.ruleToRuleNode.keySet();
	}

	/**
	 * @see #getAntecedentNeighbors(BaseRule, MatchStrategy)
	 */
	public Map<BaseRule, Set<Match>> getAntecedentNeighbors(BaseRule aRule) {
		return this.getAntecedentNeighbors(aRule, EnumSet.noneOf(MatchFlag.class));
	}

	/**
	 * Calculate the antecedent neighbors of this rule. That means all the other
	 * rules in {@code store} whose consequent matches this rule's antecedent. Note
	 * that it also adds the same information to the neighbor.
	 * 
	 * @return A mapping from a neighbor rulenode and the way its consequent matches
	 *         this rule's antecedent.
	 */
	public Map<BaseRule, Set<Match>> getAntecedentNeighbors(BaseRule aRule, EnumSet<MatchFlag> aConfig) {
		MatchNode aRuleNode = this.ruleToRuleNode.get(aRule);

		assert aRuleNode != null;

		Map<BaseRule, Set<Match>> newMapping = BaseRule.getMatches(aRule, this.getRules(), true, aConfig);

		for (Map.Entry<BaseRule, Set<Match>> entry : newMapping.entrySet()) {
			aRuleNode.setAntecedentNeighbor(entry.getKey(), Match.invertAll(entry.getValue()));
			this.ruleToRuleNode.get(entry.getKey()).setConsequentNeighbor(aRule, entry.getValue());
		}

		return newMapping;

	}

	public void addToNewMapping(Map<BaseRule, Set<Match>> newMapping, Map.Entry<BaseRule, Match> entryToAdd) {
		// check if rule already has entry in newMapping
		if (!newMapping.containsKey(entryToAdd.getKey())) {
			newMapping.put(entryToAdd.getKey(), new HashSet<>());
		}
		Set<Match> matches = newMapping.get(entryToAdd.getKey());
		matches.add(entryToAdd.getValue());
	}

	/**
	 * @see #getConsequentNeighbors(BaseRule, MatchStrategy)
	 */
	public Map<BaseRule, Set<Match>> getConsequentNeighbors(BaseRule aRule) {
		return this.getConsequentNeighbors(aRule, EnumSet.noneOf(MatchFlag.class));

	}

	/**
	 * Calculate the consequent neighbors of this rule. That means all the other
	 * rules in {@code store} whose antecedent matches this rule's consequent. Note
	 * that it also adds the same information to the neighbor.<br />
	 * 
	 * This method is cached to improve performance.
	 * 
	 * @return A mapping from a neighbor rulenode and the way its antecedent matches
	 *         this rule's consequent.
	 */
	public Map<BaseRule, Set<Match>> getConsequentNeighbors(BaseRule aRule, EnumSet<MatchFlag> aConfig) {
		MatchNode aRuleNode = this.ruleToRuleNode.get(aRule);

		assert aRuleNode != null;

		Map<BaseRule, Set<Match>> newMapping = BaseRule.getMatches(aRule, this.getRules(), false, aConfig);

		for (Map.Entry<BaseRule, Set<Match>> entry : newMapping.entrySet()) {
			aRuleNode.setConsequentNeighbor(entry.getKey(), Match.invertAll(entry.getValue()));
			this.ruleToRuleNode.get(entry.getKey()).setAntecedentNeighbor(aRule, entry.getValue());
		}

		return newMapping;
	}

	/**
	 * Resets all the rules in this store. I.e. removes the caching related to who's
	 * the neighbor of who.
	 */
	public void reset() {
		for (MatchNode r : this.ruleToRuleNode.values()) {
			r.reset();
		}
	}

	public void printGraphVizCode(ReasonerPlan aPlan) {
		LOG.info(getGraphVizCode(aPlan, false));
	}

	/**
	 * Prints all the rules and the connections between them in GraphViz encoding.
	 * Use code in: {@link https://dreampuf.github.io/GraphvizOnline/}
	 */
	public void printGraphVizCode(ReasonerPlan aPlan, boolean urlOnly) {
		LOG.info(getGraphVizCode(aPlan, urlOnly));
	}

	public String getGraphVizCode(ReasonerPlan aPlan, boolean urlOnly) {

		String color = "red";
		String width = "2";

		StringBuilder sb = new StringBuilder();

		sb.append("strict digraph {\n");
		Map<BaseRule, String> ruleToName = new HashMap<>();

		for (MatchNode r : ruleToRuleNode.values()) {

			String currentName = ruleToName.get(r.getRule());
			if (currentName == null) {
				currentName = /* "rule" + ruleNumber; */ generateName(r.getRule());
				assert !currentName.isEmpty();
				String replaceAll = toStringRule(r.getRule()).replaceAll("\\\"", "\\\\\"");

				// check the colouring
				String pen = "";
				if (aPlan != null) {
					RuleNode rn = aPlan.getRuleNodeForRule(r.getRule());
					if (rn != null) {
						pen = "color=\"" + color + "\", penwidth=\"" + width + "\",";
					}
				}

				String shape = "shape=\"circle\"";
				if (r.getRule() instanceof ProactiveRule) {
					shape = "shape=\"doublecircle\"";
				}

				sb.append(currentName).append("[").append(shape).append(pen).append("tooltip=").append("\"")
						.append(replaceAll).append("\"").append("]").append("\n");
				ruleToName.put(r.getRule(), currentName);

			}

			Map<BaseRule, Set<Match>> antecedentNeighbors = r.getAntecedentNeighbors();
			Set<BaseRule> anteNeigh = antecedentNeighbors.keySet();
			String neighName;

			for (BaseRule neighR : anteNeigh) {
				neighName = ruleToName.get(neighR);

				if (neighName == null) {
					neighName = /* "rule" + ruleNumber; */ generateName(neighR);
					assert !neighName.isEmpty();
					String replaceAll = toStringRule(neighR).replaceAll("\\\"", "\\\\\"");

					// check the colouring
					String pen = "";
					if (aPlan != null) {
						RuleNode rn = aPlan.getRuleNodeForRule(neighR);
						if (rn != null) {
							pen = "color=\"" + color + "\", penwidth=\"" + width + "\",";
						}
					}

					String shape = "shape=\"circle\"";
					if (neighR instanceof ProactiveRule) {
						shape = "shape=\"doublecircle\"";
					}

					sb.append(neighName).append("[").append(shape).append(pen).append("tooltip=").append("\"")
							.append(replaceAll).append("\"").append("]").append("\n");

					ruleToName.put(neighR, neighName);
				}

				String pen = "";
				if (aPlan != null) {
					RuleNode s = aPlan.getRuleNodeForRule(r.getRule());
					RuleNode d = aPlan.getRuleNodeForRule(neighR);
					if (s != null && d != null)
						pen = "color=\"" + color + "\", penwidth=\"" + width + "\"";
				}

				int nrOfMatches = antecedentNeighbors.get(neighR).size();

				sb.append(neighName).append(BaseRule.ARROW).append(currentName).append("[label=").append(nrOfMatches)
						.append(" ").append(pen).append("]").append("\n");

			}
		}

		sb.append("}");

		return "Visualize on website: https://dreampuf.github.io/GraphvizOnline/#"
				+ URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8).replaceAll("\\+", "%20")
				+ (urlOnly ? "" : "\n" + sb.toString());
	}

	private String toStringRule(BaseRule neighR) {
		return neighR.getAntecedent() + " -> " + neighR.getConsequent();
	}

	private String trimAtLength(String aString, int aLength) {

		String newString = aString.substring(0, Math.min(aString.length(), aLength));

		if (newString.length() < aString.length()) {
			newString = newString + "...";
		}

		return newString;
	}

	/**
	 * Generates a shorter name for a rule while still being reasonably
	 * recognizable.
	 * 
	 * @param r
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private String generateName(BaseRule r) {

		StringBuilder sb = new StringBuilder();

		if (!r.getAntecedent().isEmpty()) {
			for (TriplePattern tp : r.getAntecedent()) {
				sb.append(generateName(tp)).append(" ");
			}
		}

		String antecedent = trimAtLength(sb.toString(), MAX_STR_LENGTH);

		sb = new StringBuilder();

		if (!r.getConsequent().isEmpty()) {
			for (TriplePattern tp : r.getConsequent()) {
				sb.append(generateName(tp)).append(" ");
			}
			if (sb.length() > 0)
				sb.deleteCharAt(sb.length() - 1);
		}

		String consequent = trimAtLength(sb.toString(), MAX_STR_LENGTH);

		String name = r.getName();
		MessageDigest digest;
		byte[] encodedhash = new byte[0];
		try {
			digest = MessageDigest.getInstance("SHA-256");
			encodedhash = digest.digest(name.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			LOG.error("{}", e);
		}

		return "\"" + bytesToHex(encodedhash) + "\\n" + antecedent + "->\\n" + consequent + "\"";
	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private String generateName(TriplePattern tp) {

		String name = BaseRule.EMPTY;
		if (tp.getPredicate().toString().contains("type") && !tp.getObject().isVariable())
			if (tp.getObject().isLiteral())
				name += tp.getObject().toString() + " ";
			else {
				URI uri = URI.create(tp.getObject().getURI());
				name += uriToString(uri);
			}
		else {
			if (!tp.getSubject().isVariable())

				if (tp.getSubject().isLiteral())
					name += tp.getSubject().toString() + " ";
				else {
					URI uri = URI.create(tp.getSubject().getURI());
					name += uriToString(uri);
				}
			if (!tp.getPredicate().isVariable()) {
				URI uri = URI.create(tp.getPredicate().getURI());
				name += uriToString(uri);
			}
			if (!tp.getObject().isVariable()) {
				if (tp.getObject().isLiteral())
					name += tp.getObject().toString() + " ";
				else {
					URI uri = URI.create(tp.getObject().getURI());
					name += uriToString(uri);
				}
			}
		}

		if (name.endsWith(" "))
			name = name.substring(0, name.length() - 1);

		if (name.isEmpty())
			name = "unknown";

		name = name.replaceAll("\\\"", "\\\\\"");

		return name;
	}

	private String uriToString(URI uri) {

		String text;
		if (uri.getFragment() == null || uri.getFragment().isEmpty()) {
			String[] segments = uri.getPath().split("/");
			text = segments[segments.length - 1] + " ";
		} else
			text = uri.getFragment();
		return text;
	}

}
