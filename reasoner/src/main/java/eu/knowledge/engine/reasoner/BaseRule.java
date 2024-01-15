package eu.knowledge.engine.reasoner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleNode;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class BaseRule {

	private static final Logger LOG = LoggerFactory.getLogger(BaseRule.class);

	public static final String EMPTY = "";

	public static final String ARROW = "->";

	public static enum MatchStrategy {
		FIND_ALL_MATCHES, FIND_ONLY_BIGGEST_MATCHES, FIND_ONLY_FULL_MATCHES
	}

	public static class TrivialBindingSetHandler implements TransformBindingSetHandler {

		private Set<TriplePattern> consequent;

		public TrivialBindingSetHandler(Set<TriplePattern> aConsequent) {
			this.consequent = aConsequent;
		}

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			BindingSet newBS = new BindingSet();

			Binding newB;

			Set<Var> vars = BaseRule.getVars(this.consequent);
			for (Binding b : bs) {
				newB = new Binding();
				for (Var v : vars) {
					if (b.containsKey(v)) {
						newB.put(v, b.get(v));
					} else {
						throw new IllegalArgumentException(
								"Not all variables in the consequent are available in the antecedent of the rule. This type of rule should use a custom BindingHandler.");
					}
				}
				newBS.add(newB);
			}

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			future.complete(newBS);
			return future;
		}
	}

	/**
	 * The antecedent of this rule. This set of triple patterns represents the type
	 * of data that is required for this rule to apply.
	 */
	private Set<TriplePattern> antecedent;

	/**
	 * The consequent of this rule. This set of triple patterns represent the type
	 * of data that results after applying this rule.
	 */
	private Set<TriplePattern> consequent;

	private String name = "";

	protected BaseRule(String aName, Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		this(anAntecedent, aConsequent);
		assert aName != null;
		this.name = aName;
	}

	protected BaseRule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {

		if (anAntecedent == null || aConsequent == null)
			throw new IllegalArgumentException("A rule should have both antecedent and consequent non-null.");

		if (anAntecedent.isEmpty() && aConsequent.isEmpty())
			throw new IllegalArgumentException("A rule should not have both antecedent and consequent empty.");

		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
	}

	public static Set<Var> getVars(Set<TriplePattern> aPattern) {
		Set<Var> vars = new HashSet<Var>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVariables());
		}
		return vars;
	}

	public Set<TriplePattern> getAntecedent() {
		return this.antecedent;
	}

	public Set<TriplePattern> getConsequent() {
		return this.consequent;
	}

	public Set<Match> consequentMatches(Set<TriplePattern> anAntecedent, MatchStrategy aMatchStrategy) {
		if (!this.consequent.isEmpty())
			return matches(anAntecedent, this.consequent, aMatchStrategy);
		return new HashSet<>();
	}

	public Set<Match> antecedentMatches(Set<TriplePattern> aConsequent, MatchStrategy aMatchStrategy) {
		if (!this.antecedent.isEmpty())
			return matches(aConsequent, this.antecedent, aMatchStrategy);
		return new HashSet<>();
	}

	/**
	 * FInd the biggest matches bewteen two graph patterns.
	 * 
	 * @param aFirstPattern
	 * @param aSecondPattern
	 * @param aMatchStrategy
	 * @return
	 */
	public static Set<Match> matches(Set<TriplePattern> aFirstPattern, Set<TriplePattern> aSecondPattern,
			MatchStrategy aMatchStrategy) {

		assert aFirstPattern != null;
		assert aSecondPattern != null;
		assert !aFirstPattern.isEmpty();
		assert !aSecondPattern.isEmpty();

		long start = System.currentTimeMillis();

		List<Match> allMatches = new ArrayList<>();

		// first find all triples in the consequent that match each triple in the
		// antecedent
		Map<TriplePattern, List<Match>> matchesPerTriple = new HashMap<>();
		List<Match> findMatches;
		for (TriplePattern anteTriple : aFirstPattern) {
			// find all possible matches of the current antecedent triple in the consequent
			findMatches = findMatches(anteTriple, aSecondPattern);
			if (!findMatches.isEmpty())
				matchesPerTriple.put(anteTriple, findMatches);
		}

		// if not every triple pattern can be matched, we stop the process if we require
		// a full match.
		if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)
				&& matchesPerTriple.keySet().size() < aFirstPattern.size())
			return new HashSet<>();

		// next, correctly combine all found matches
		List<Match> biggestMatches = new ArrayList<>();
		List<Match> smallerMatches = new ArrayList<>();
		Match mergedMatch = null;
		List<Match> matches = null;
		List<Match> toBeAddedToBiggestMatches = null, toBeAddedToSmallerMatches = null;
		Set<Integer> toBeDemotedMatchIndices = null;

		Iterator<Map.Entry<TriplePattern, List<Match>>> matchIter = matchesPerTriple.entrySet().iterator();

		// always add first matches
		if (matchIter.hasNext()) {
			biggestMatches.addAll(matchIter.next().getValue());
		}

		int idx = 0;
		while (matchIter.hasNext()) {
			idx++;
			LOG.trace("Processing triple pattern {}/{} with {} biggest and {} smaller matches.", idx,
					matchesPerTriple.size(), biggestMatches.size(), smallerMatches.size());

			long innerStart = System.currentTimeMillis();

			Map.Entry<TriplePattern, List<Match>> entry = matchIter.next();

			// keep a set of new/remove matches, so we can add/remove them at the end of
			// this loop
			toBeAddedToBiggestMatches = new ArrayList<>();
			toBeAddedToSmallerMatches = new ArrayList<>();
			toBeDemotedMatchIndices = new HashSet<>();

			matches = entry.getValue();
			assert matches != null;

			for (Match m1 : matches) {
				// check if we need to merge with existing matches
				boolean hasMerged = false;
				// first check if m1 can be merged with any of the existing biggest matches.
				int biggestMatchesSize = biggestMatches.size();
				for (int i = 0; i < biggestMatchesSize; i++) {
					Match m2 = biggestMatches.get(i);
					mergedMatch = m2.merge(m1);
					if (mergedMatch != null) {
						hasMerged = true;
						toBeAddedToBiggestMatches.add(mergedMatch);
						toBeDemotedMatchIndices.add(i);
					} else if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
						toBeDemotedMatchIndices.add(i);
					}
				}

				// then check if m1 can be merged with any of the existing smaller matches
				if (!aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
					for (Match m2 : smallerMatches) {
						mergedMatch = m2.merge(m1);
						if (mergedMatch != null) {

							if (hasMerged) {
								// add to smallerMatches and sometimes to biggestMatches.
								if (isSubMatch(m2, toBeAddedToBiggestMatches)) {
									// add to smaller matches
									toBeAddedToSmallerMatches.add(mergedMatch);
								} else {
									// add to biggest matches
									toBeAddedToBiggestMatches.add(mergedMatch);
								}
							} else {
								// add to biggestMatches
								hasMerged = true;
								toBeAddedToBiggestMatches.add(mergedMatch);
							}
						}
					}
				}

				if (!hasMerged && !aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
					toBeAddedToBiggestMatches.add(m1);
				} else {
					toBeAddedToSmallerMatches.add(m1);
				}
			}

			// remove all toBeDemotedMatches from the biggestMatches and add them to the
			// smallerMatches.

			List<Integer> sortedList = new ArrayList<>(toBeDemotedMatchIndices);
			Collections.sort(sortedList, Collections.reverseOrder());
			for (int i : sortedList) {
				smallerMatches.add(biggestMatches.get(i));
				biggestMatches.remove(i);
			}

			// add all toBeAddedMatches
			biggestMatches.addAll(toBeAddedToBiggestMatches);
			smallerMatches.addAll(toBeAddedToSmallerMatches);

			long innerEnd = System.currentTimeMillis();
			toBeAddedToBiggestMatches = null;
			toBeDemotedMatchIndices = null;
			toBeAddedToSmallerMatches = null;
		}

		assert biggestMatches != null;

		long finalEnd = System.currentTimeMillis();

		if (aMatchStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)) {
			allMatches.addAll(biggestMatches);
			allMatches.addAll(smallerMatches);
		} else if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES)
				|| aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
			allMatches.addAll(biggestMatches);
		}

		return new HashSet<>(allMatches);
	}

	/**
	 * Go over all matches in toBeaddedToBiggesetMatches and check if aMatch is a
	 * subMatch of one of those.
	 * 
	 * @param aMatch
	 * @param toBeAddedToBiggestMatches
	 * @return
	 */
	private static boolean isSubMatch(Match aMatch, List<Match> toBeAddedToBiggestMatches) {

		for (Match m : toBeAddedToBiggestMatches) {
			if (m.isSubMatch(aMatch)) {
				return true;
			}
		}
		return false;
	}

	private static List<Match> findMatches(TriplePattern antecedent, Set<TriplePattern> consequent) {

		assert consequent != null;
		assert antecedent != null;
		assert !consequent.isEmpty();

		List<Match> matchingTriplePatterns = new ArrayList<>();
		Map<TripleNode, TripleNode> map;
		for (TriplePattern tp : consequent) {
			map = antecedent.findMatches(tp);
			if (map != null) {
				matchingTriplePatterns.add(new Match(antecedent, tp, map));
			}
		}

		assert matchingTriplePatterns != null;
		return matchingTriplePatterns;
	}

	public boolean isProactive() {
		return this instanceof ProactiveRule;
	}

	@Override
	public String toString() {
		return antecedent + " -> " + consequent + (!this.name.isEmpty() ? "(" + this.name + ")" : "");
	}

	public Set<Var> getVars() {

		Set<Var> vars = new HashSet<>();

		if (this.antecedent != null)
			vars.addAll(BaseRule.getVars(this.antecedent));

		if (this.consequent != null)
			vars.addAll(BaseRule.getVars(this.consequent));

		return vars;
	}

	public static List<Rule> read(String testRules) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(RuleStore.class.getResourceAsStream(testRules)));
		String line;
		List<Rule> rules = new ArrayList<>();
		boolean isAntecedent = true;
		Set<TriplePattern> antecedent = new HashSet<>();
		Set<TriplePattern> consequent = new HashSet<>();
		while ((line = br.readLine()) != null) {

			String trimmedLine = line.trim();

			if (trimmedLine.equals(EMPTY)) {
				if (!antecedent.isEmpty() || !consequent.isEmpty()) {
					// start a new rule
					rules.add(new Rule(antecedent, consequent));
					antecedent = new HashSet<>();
					consequent = new HashSet<>();
					isAntecedent = true;
				} else {
					// ignore
				}
			} else if (trimmedLine.equals(ARROW)) {
				// toggle between antecedent to consequent
				isAntecedent = !isAntecedent;
			} else {
				// triple
				if (isAntecedent) {
					antecedent.add(new TriplePattern(trimmedLine));
				} else {
					consequent.add(new TriplePattern(trimmedLine));
				}
			}
		}
		if (!antecedent.isEmpty() || !consequent.isEmpty()) {
			// start a new rule
			rules.add(new Rule(antecedent, consequent));
			antecedent = new HashSet<>();
			consequent = new HashSet<>();
		}

		return rules;
	}

	protected void setName(String aName) {
		assert aName != null;
		this.name = aName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((antecedent == null) ? 0 : antecedent.hashCode());
		result = prime * result + ((consequent == null) ? 0 : consequent.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseRule other = (BaseRule) obj;
		if (antecedent == null) {
			if (other.antecedent != null)
				return false;
		} else if (!antecedent.equals(other.antecedent))
			return false;
		if (consequent == null) {
			if (other.consequent != null)
				return false;
		} else if (!consequent.equals(other.consequent))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
