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
import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
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
	private Map<BaseRule, MatchNode> ruleToMatchNode;

	/**
	 * Instantiate an empty rule store.
	 */
	public RuleStore() {
		ruleToMatchNode = new HashMap<>();
	}

	public void addRule(BaseRule aRule) {
		MatchNode aMatchNode = new MatchNode(aRule);
		this.ruleToMatchNode.put(aRule, aMatchNode);
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

		return this.ruleToMatchNode.keySet();
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
		MatchNode aMatchNode = this.ruleToMatchNode.get(aRule);

		assert aMatchNode != null;

		// calculate matches
		Set<CombiMatch> combiMatches = BaseRule.getMatches(aRule, this.getRules(), true, aConfig);

		// store combi matches
		aMatchNode.setAntecedentCombiMatches(combiMatches);

		// store normal matches
		Map<BaseRule, Set<Match>> newMapping = convertToMapping(combiMatches);

		for (Map.Entry<BaseRule, Set<Match>> entry : newMapping.entrySet()) {
			aMatchNode.setAntecedentNeighbor(entry.getKey(), Match.invertAll(entry.getValue()));
			this.ruleToMatchNode.get(entry.getKey()).setConsequentNeighbor(aRule, entry.getValue());
		}

		return newMapping;

	}

	public Set<CombiMatch> getAntecedentCombiMatches(BaseRule aRule) {
		assert aRule != null;
		MatchNode mn = this.ruleToMatchNode.get(aRule);
		return mn.getAntecedentCombiMatches();
	}

	public Set<CombiMatch> getConsequentCombiMatches(BaseRule aRule) {
		assert aRule != null;
		MatchNode mn = this.ruleToMatchNode.get(aRule);
		return mn.getConsequentCombiMatches();
	}

	private Map<BaseRule, Set<Match>> convertToMapping(Set<CombiMatch> someMatches) {
		var mapping = new HashMap<BaseRule, Set<Match>>();

		for (CombiMatch cm : someMatches) {
			for (Map.Entry<BaseRule, Set<Match>> entry : cm.entrySet()) {
				// get rule match set
				var ruleMatchSet = mapping.get(entry.getKey());
				if (ruleMatchSet == null) {
					ruleMatchSet = new HashSet<Match>();
					mapping.put(entry.getKey(), ruleMatchSet);
				}
				ruleMatchSet.addAll(entry.getValue());
			}
		}
		return mapping;
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
		MatchNode aMatchNode = this.ruleToMatchNode.get(aRule);

		assert aMatchNode != null;

		// calculate matches
		Set<CombiMatch> combiMatches = BaseRule.getMatches(aRule, this.getRules(), false, aConfig);

		// store combi matches
		aMatchNode.setConsequentCombiMatches(combiMatches);

		// store normal matches
		Map<BaseRule, Set<Match>> newMapping = convertToMapping(combiMatches);

		for (Map.Entry<BaseRule, Set<Match>> entry : newMapping.entrySet()) {
			aMatchNode.setConsequentNeighbor(entry.getKey(), Match.invertAll(entry.getValue()));
			this.ruleToMatchNode.get(entry.getKey()).setAntecedentNeighbor(aRule, entry.getValue());
		}

		return newMapping;
	}

	/**
	 * Resets all the rules in this store. I.e. removes the caching related to who's
	 * the neighbor of who.
	 */
	public void reset() {
		for (MatchNode r : this.ruleToMatchNode.values()) {
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
		String width = "1.5";

		StringBuilder sb = new StringBuilder();

		sb.append("strict digraph {\n");
		Map<BaseRule, String> ruleToId = new HashMap<>();

		for (MatchNode r : ruleToMatchNode.values()) {

			String currentId = ruleToId.get(r.getRule());
			if (currentId == null) {
				currentId = Integer.toHexString(System.identityHashCode(r));
				assert !currentId.isEmpty();
				String replaceAll = toStringRule(r.getRule()).replaceAll("\\\"", "\\\\\"");

				// check the colouring
				String pen = "";
				if (aPlan != null) {
					RuleNode rn = aPlan.getRuleNodeForRule(r.getRule());
					if (rn != null) {
						pen = "color=\"" + color + "\", penwidth=\"" + width + "\",";
					}
				}

				String shape = "shape=\"rect\"";
				String doubleShape = "";
				if (r.getRule() instanceof ProactiveRule) {
					doubleShape = ", peripheries=2";
				}

				sb.append(q(currentId)).append("[").append(shape).append(", ").append(pen).append("tooltip=")
						.append("\"").append(replaceAll).append("\"").append(doubleShape).append(", label=")
						.append(generateName(r.getRule())).append("]").append("\n");
				ruleToId.put(r.getRule(), Integer.toHexString(System.identityHashCode(r)));

			}

			Map<BaseRule, Set<Match>> antecedentNeighbors = r.getAntecedentNeighbors();
			Set<BaseRule> anteNeigh = antecedentNeighbors.keySet();
			String neighId;

			for (BaseRule neighR : anteNeigh) {
				neighId = ruleToId.get(neighR);

				if (neighId == null) {
					neighId = Integer.toHexString(System.identityHashCode(neighR));
					assert !neighId.isEmpty();
					String ruleString = toStringRule(neighR).replaceAll("\\\"", "\\\\\"");

					// check the colouring
					String pen = "";
					if (aPlan != null) {
						RuleNode rn = aPlan.getRuleNodeForRule(neighR);
						if (rn != null) {
							pen = "color=\"" + color + "\", penwidth=\"" + width + "\",";
						}
					}

					String shape = "shape=\"rect\"";
					String doubleShape = "";
					if (neighR instanceof ProactiveRule) {
						doubleShape = ", peripheries=2";
					}

					sb.append(q(neighId)).append("[").append(shape).append(pen).append("tooltip=").append("\"")
							.append(ruleString).append("\"").append(doubleShape).append(", label=")
							.append(generateName(neighR)).append("").append("]").append("\n");

					ruleToId.put(neighR, neighId);
				}

				String pen = "";
				if (aPlan != null) {
					RuleNode s = aPlan.getRuleNodeForRule(r.getRule());
					RuleNode d = aPlan.getRuleNodeForRule(neighR);
					if (s != null && d != null)
						pen = "color=\"" + color + "\", penwidth=\"" + width + "\"";
				}

				int nrOfMatches = antecedentNeighbors.get(neighR).size();

				sb.append(q(neighId)).append("->").append(q(currentId)).append("[label=").append(nrOfMatches)
						.append(" ").append(pen).append("]").append("\n");

			}
		}

		sb.append("}");

		return "Visualize on website: https://dreampuf.github.io/GraphvizOnline/#"
				+ URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8).replaceAll("\\+", "%20")
				+ (urlOnly ? "" : "\n" + sb.toString());
	}

	private String q(String aString) {
		return "\"" + aString + "\"";
	}

	private String toStringRule(BaseRule neighR) {
		return neighR.getAntecedent() + " &rarr; " + neighR.getConsequent();
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
		String encodedhash = "unknown";
		try {
			digest = MessageDigest.getInstance("SHA-256");

//			encodedhash = digest.digest(name.getBytes(StandardCharsets.UTF_8));
			encodedhash = Integer.toHexString(System.identityHashCode(r));
		} catch (NoSuchAlgorithmException e) {
			LOG.error("{}", e);
		}
		// bytesToHex(...)
		return "\"" + antecedent + "&rarr;\\n" + consequent + "\"";
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
