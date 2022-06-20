/**
 * 
 */
package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private Set<Rule> rules;

	/**
	 * Instantiate an empty rule store.
	 */
	public RuleStore() {
		rules = new HashSet<Rule>();
	}

	public void addRule(Rule aRule) {
		aRule.setStore(this);
		this.rules.add(aRule);
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
		return this.rules;
	}

	/**
	 * Prints all the rules and the connections between them in GraphViz encoding.
	 */
	public void printGraphVizCode() {

		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("digraph {").append("\n");
		Map<Rule, String> ruleToName = new HashMap<>();

		int ruleNumber = 1;

		for (Rule r : rules) {

			String currentName = ruleToName.get(r);
			if (currentName == null) {
				currentName = /* "rule" + ruleNumber; */ generateName(r);
				assert !currentName.isEmpty();
				ruleNumber++;
				String replaceAll = toStringRule(r).replaceAll("\\\"", "\\\\\"");

				sb.append(currentName).append("[").append("tooltip=").append("\"").append(replaceAll).append("\"")
						.append("]").append("\n");
				ruleToName.put(r, currentName);
			}

			Set<Rule> anteNeigh = r.getAntecedentNeighbors().keySet();
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

				sb.append(neighName).append("->").append(currentName).append("\n");

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

		String name = "";
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

	/**
	 * Resets all the rules in this store. I.e. removes the caching related to who's
	 * the neighbor of who.
	 */
	public void reset() {
		for (Rule r : this.rules) {
			r.reset();
		}
	}
}
