package eu.knowledge.engine.reasoner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class Rule {

	public static enum MatchStrategy {
		FIND_ALL_MATCHES, FIND_ONLY_BIGGEST_MATCHES, FIND_ONLY_FULL_MATCHES
	}

	public Set<TriplePattern> antecedent;
	public Set<TriplePattern> consequent;

	public BindingSetHandler bindingSetHandler;

	public Rule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent, BindingSetHandler aBindingSetHandler) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = aBindingSetHandler;
	}

	public Rule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = new BindingSetHandler() {
			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {

				BindingSet newBS = new BindingSet();

				Binding newB;

				Set<Var> vars = Rule.this.getVars(Rule.this.consequent);
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
		};
	}

	public Set<Var> getVars(Set<TriplePattern> aPattern) {
		Set<Var> vars = new HashSet<Var>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVariables());
		}
		return vars;
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

	public BindingSetHandler getBindingSetHandler() {
		return bindingSetHandler;
	}

	/**
	 * Try if we can match with better heuristics: try to work towards full matches
	 * of the graph pattern using all the rules at once. This way, hopefully, a lot
	 * of matches can already be discarded and do not slow down the process further
	 * down the line.
	 * 
	 * How do we know whether we should match the antecedent or consequent of those
	 * rules?
	 * 
	 * @param aFirstPattern
	 * @param allRules
	 * @param aMatchStrategy
	 * @return
	 */
	public static Set<Map<Rule, Match>> findMatches(Set<TriplePattern> aFirstPattern, List<Rule> allRules,
			MatchStrategy aMatchStrategy, boolean forConsequent) {

		List<Map<Rule, Match>> allMatches = new ArrayList<>();

		Map<TriplePattern, Set<Map<Rule, Match>>> matchesPerTriplePerRule = getMatchesPerTriplePerRule(aFirstPattern,
				allRules, forConsequent);

		// if not every triple pattern can be matched, we stop the process if we require
		// a full match.
		if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)
				&& matchesPerTriplePerRule.keySet().size() < aFirstPattern.size())
			return new HashSet<>();

		// now combine all found matches.
		List<Map<Rule, Match>> biggestRuleMatches = new ArrayList<>();
		List<Map<Rule, Match>> smallerMatches = new ArrayList<>();
		Match mergedMatch = null;
		List<Map<Rule, Match>> toBeAddedToBiggestMatches = null, toBeAddedToSmallerMatches = null;
		Set<Integer> toBeDemotedMatchIndices = null;

		Iterator<Map.Entry<TriplePattern, Set<Map<Rule, Match>>>> matchesPerTripleIter = matchesPerTriplePerRule.entrySet()
				.iterator();

		// always add all matches of first triple
		if (matchesPerTripleIter.hasNext()) {
			biggestRuleMatches.addAll(matchesPerTripleIter.next().getValue());
		}

		while (matchesPerTripleIter.hasNext()) {

			Set<Map<Rule, Match>> matchesForCurrentTriple = matchesPerTripleIter.next().getValue();

			// keep a set of new/removed matches, so we can add/remove them at the end of
			// this loop

			assert matchesForCurrentTriple != null;

			toBeAddedToBiggestMatches = new ArrayList<>();
			toBeAddedToSmallerMatches = new ArrayList<>();
			toBeDemotedMatchIndices = new HashSet<>();
			for (Map<Rule, Match> matchForCurrentTriple : matchesForCurrentTriple) {
				// see if this next match can be merged with one of the existing biggest matches
				for (Entry<Rule, Match> m1entry : matchForCurrentTriple.entrySet()) {
					Match m1 = m1entry.getValue();

					// check if we need to merge with existing matches
					boolean hasMerged = false;
					// first check if m1 can be merged with any of the existing biggest matches.
					for (int i = 0; i < biggestRuleMatches.size(); i++) {

						Map<Rule, Match> existingBiggestRuleMatch = biggestRuleMatches.get(i);

						Match m2 = existingBiggestRuleMatch.get(m1entry.getKey());

						if (m2 != null) {

							mergedMatch = m2.merge(m1);
							if (mergedMatch != null) {
								hasMerged = true;
								HashMap<Rule, Match> newRuleMatch = new HashMap<>(existingBiggestRuleMatch);
								newRuleMatch.put(m1entry.getKey(), mergedMatch);
								toBeAddedToBiggestMatches.add(newRuleMatch);
								toBeDemotedMatchIndices.add(i);
							} else if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
								toBeDemotedMatchIndices.add(i);
							}
						} else {
							toBeDemotedMatchIndices.add(i);
							HashMap<Rule, Match> newRuleMatch = new HashMap<>(existingBiggestRuleMatch);
							newRuleMatch.put(m1entry.getKey(), m1);
							if (!aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
								toBeAddedToBiggestMatches.add(newRuleMatch);
							} else {
								toBeAddedToSmallerMatches.add(newRuleMatch);
							}
						}
					}

					// then check if m1 can be merged with any of the existing smaller matches
					if (!aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {

						for (Map<Rule, Match> ruleMatch2 : smallerMatches) {
							Match m2 = ruleMatch2.get(m1entry.getKey());

							if (m2 != null) {

								mergedMatch = m2.merge(m1);
								if (mergedMatch != null) {

									if (hasMerged) {
										// add to smallerMatches and sometimes to biggestMatches.
										if (isSubMatch2(ruleMatch2, toBeAddedToBiggestMatches)) {
											// add to smaller matches
											HashMap<Rule, Match> newRuleMatch = new HashMap<>(ruleMatch2);
											newRuleMatch.put(m1entry.getKey(), mergedMatch);
											toBeAddedToSmallerMatches.add(newRuleMatch);
										} else {
											// add to biggest matches
											HashMap<Rule, Match> newRuleMatch = new HashMap<>(ruleMatch2);
											newRuleMatch.put(m1entry.getKey(), mergedMatch);
											toBeAddedToBiggestMatches.add(newRuleMatch);
										}
									} else {
										// add to biggestMatches
										hasMerged = true;
										HashMap<Rule, Match> newRuleMatch = new HashMap<>(ruleMatch2);
										newRuleMatch.put(m1entry.getKey(), mergedMatch);
										toBeAddedToBiggestMatches.add(newRuleMatch);
									}
								}
							} else {

								if (hasMerged) {

									// TODO probably we need to check for isSubMatch2() here too?
									// yes, if the current ruleMatch2 is a submatch of one of the
									// toBeAddedToBiggestMatches

									if (isSubMatch2(ruleMatch2, toBeAddedToBiggestMatches)) {
										HashMap<Rule, Match> newRuleMatch = new HashMap<>(ruleMatch2);
										newRuleMatch.put(m1entry.getKey(), m1);
										toBeAddedToSmallerMatches.add(newRuleMatch);
									} else {
										HashMap<Rule, Match> newRuleMatch = new HashMap<>(ruleMatch2);
										newRuleMatch.put(m1entry.getKey(), m1);
										toBeAddedToBiggestMatches.add(newRuleMatch);
									}
								} else {
									HashMap<Rule, Match> newRuleMatch = new HashMap<>(ruleMatch2);
									newRuleMatch.put(m1entry.getKey(), m1);
									toBeAddedToBiggestMatches.add(newRuleMatch);
								}
							}
						}
					}

					if (!hasMerged && !aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
						toBeAddedToBiggestMatches.add(matchForCurrentTriple);
					} else {
						toBeAddedToSmallerMatches.add(matchForCurrentTriple);
					}
				}
			}
			// remove all toBeDemotedMatches from the biggestMatches and add them to the
			// smallerMatches.
			
			List<Integer> sortedList = new ArrayList<>(toBeDemotedMatchIndices);
			Collections.sort(sortedList, Collections.reverseOrder());
			for (int i : sortedList) {
				smallerMatches.add(biggestRuleMatches.get(i));
				biggestRuleMatches.remove(i);
			}
			
			// add all toBeAddedMatches
			biggestRuleMatches.addAll(toBeAddedToBiggestMatches);
			smallerMatches.addAll(toBeAddedToSmallerMatches);
			
			long innerEnd = System.currentTimeMillis();
			toBeAddedToBiggestMatches = null;
			toBeDemotedMatchIndices = null;
			toBeAddedToSmallerMatches = null;

		}
		if (aMatchStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)) {
			allMatches.addAll(biggestRuleMatches);
			allMatches.addAll(smallerMatches);
		} else if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES)
				|| aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)) {
			allMatches.addAll(biggestRuleMatches);
		}

		return new HashSet<>(allMatches);

	}

	private static boolean isSubMatch2(Map<Rule, Match> aRuleMatch, List<Map<Rule, Match>> toBeAddedToBiggestMatches) {

		for (Map<Rule, Match> ruleMatch : toBeAddedToBiggestMatches) {
			boolean foundAll = true;
			for (Entry<Rule, Match> rm : aRuleMatch.entrySet()) {

				if (!ruleMatch.containsKey(rm.getKey()) || !ruleMatch.get(rm.getKey()).isSubMatch(rm.getValue())) {
					foundAll = false;
					break;
				}
			}
			if (foundAll)
				return true;
		}
		return false;
	}

	public static Map<TriplePattern, Set<Map<Rule, Match>>> getMatchesPerTriplePerRule(Set<TriplePattern> aFirstPattern,
			List<Rule> allRules, boolean useConsequent) {
		Map<TriplePattern, Set<Map<Rule, Match>>> matchesPerRule = new HashMap<>();

		for (Rule r : allRules) {
			// first find all triples in the consequent that match each triple in the
			// antecedent
			Set<Match> foundMatches;
			for (TriplePattern anteTriple : aFirstPattern) {
				// find all possible matches of the current antecedent triple in the consequent
				if (useConsequent ? !r.consequent.isEmpty() : !r.antecedent.isEmpty()) {
					foundMatches = findMatches(anteTriple, useConsequent ? r.consequent : r.antecedent);
					if (!foundMatches.isEmpty()) {

						Set<Map<Rule, Match>> ruleMatches = matchesPerRule.get(anteTriple);
						if (ruleMatches == null) {
							ruleMatches = new HashSet<>();
							matchesPerRule.put(anteTriple, ruleMatches);
						}
						for (Match m : foundMatches) {
							HashMap<Rule, Match> hashMap = new HashMap<Rule, Match>();
							hashMap.put(r, m);
							ruleMatches.add(hashMap);
						}
					}
				}
			}
		}
		return matchesPerRule;
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

		List<Match> allMatches = new LinkedList<>();

		// first find all triples in the consequent that match each triple in the
		// antecedent
		Map<TriplePattern, Set<Match>> matchesPerTriple = new HashMap<>();
		Set<Match> findMatches;
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
		Set<Match> matches = null;
		List<Match> toBeAddedToBiggestMatches = null, toBeAddedToSmallerMatches = null;
		Set<Integer> toBeDemotedMatchIndices = null;

		Iterator<Map.Entry<TriplePattern, Set<Match>>> matchIter = matchesPerTriple.entrySet().iterator();

		// always add first matches
		if (matchIter.hasNext()) {
			biggestMatches.addAll(matchIter.next().getValue());
		}

		while (matchIter.hasNext()) {

			long innerStart = System.currentTimeMillis();

			Map.Entry<TriplePattern, Set<Match>> entry = matchIter.next();

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
				for (int i = 0; i < biggestMatches.size(); i++) {
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

	private static Set<Match> findMatches(TriplePattern antecedent, Set<TriplePattern> consequent) {

		assert consequent != null;
		assert antecedent != null;
		assert !consequent.isEmpty();

		Set<Match> matchingTriplePatterns = new HashSet<>();
		Map<Node, Node> map;
		for (TriplePattern tp : consequent) {
			map = antecedent.findMatches(tp);
			if (map != null) {
				matchingTriplePatterns.add(new Match(antecedent, tp, map));
			}
		}

		assert matchingTriplePatterns != null;
		return matchingTriplePatterns;
	}

	public Set<Var> getVars() {

		Set<Var> vars = new HashSet<>();
		;
		if (this.antecedent != null)
			vars.addAll(this.getVars(this.antecedent));

		if (this.consequent != null)
			vars.addAll(this.getVars(this.consequent));

		return vars;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((antecedent == null) ? 0 : antecedent.hashCode());
		result = prime * result + ((bindingSetHandler == null) ? 0 : bindingSetHandler.hashCode());
		result = prime * result + ((consequent == null) ? 0 : consequent.hashCode());
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
		Rule other = (Rule) obj;
		if (antecedent == null) {
			if (other.antecedent != null)
				return false;
		} else if (!antecedent.equals(other.antecedent))
			return false;
		if (bindingSetHandler == null) {
			if (other.bindingSetHandler != null)
				return false;
		} else if (!bindingSetHandler.equals(other.bindingSetHandler))
			return false;
		if (consequent == null) {
			if (other.consequent != null)
				return false;
		} else if (!consequent.equals(other.consequent))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Rule [" + (antecedent != null ? "antecedent=" + antecedent + ", " : "")
				+ (consequent != null ? "consequent=" + consequent : "") + "]";
	}

}
