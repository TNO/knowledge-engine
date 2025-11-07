package eu.knowledge.engine.reasoner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

	/**
	 * Precalculated hashcode to improve performance of the matching algorithm.
	 */
	private int hashCodeValue;

	/**
	 * A comparator to make sure the smaller matches collection is ordered from big
	 * to small.
	 */
	public static class CombiMatchSizeComparator implements Comparator<CombiMatch> {
		public int compare(CombiMatch object1, CombiMatch object2) {
			return object2.getSize() - object1.getSize();
		}
	}

	/**
	 * Some flags used during the matching process. It is targeted to the internally
	 * generated combi matches.
	 */
	public static enum MatchFlag {

		/**
		 * Only look for combi matches that fully cover the target graph pattern. Note
		 * that for backward reasoning (where we match neighbors to antecedents) this is
		 * an essential configuration property to improve performance, however, for
		 * backward reasoning (where we match consequent neighbors) this leaves out
		 * matches to neighbors. The reason is that when forward reasoning, the
		 * consequent graph pattern can match partially so only part of the forwarded
		 * data is being used.
		 */
		FULLY_COVERED,

		/**
		 * Only look for combi matches that consist of a single candidate rule.
		 */
		SINGLE_RULE,

		/**
		 * Only look for combi matches in which a candidate triple pattern can only map
		 * to a single target triple pattern. Note that setting this flag prevents some
		 * transitivity rules to correctly match (x p y . y p z . -> x p z)
		 */
		ONE_TO_ONE,

		/**
		 * Only look for the biggest combi matches and ignore combi matches that are a
		 * submatch of another combi match.
		 */
		ONLY_BIGGEST,

		/**
		 * Temp name: If the current combi match already has a triple pattern matched by
		 * one rule, this flag makes sure that a next triple pattern should always only
		 * be matched to that one rule if it exists in there and only if this one rule
		 * does not have the triple pattern will it allow other rules to match to that
		 * triple pattern.
		 */
		ONLY_NEW_RULE_WHEN_NECESSARY;

		public static final EnumSet<MatchFlag> ALL_OPTS = EnumSet.allOf(MatchFlag.class);
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
		this.hashCodeValue = this.calcHashCode();
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

	public Set<Match> consequentMatches(Set<TriplePattern> anAntecedent, EnumSet<MatchFlag> aMatchConfig) {
		if (!this.consequent.isEmpty()) {
			Rule r = new Rule(anAntecedent, new SinkBindingSetHandler() {

				@Override
				public CompletableFuture<Void> handle(BindingSet aBindingSet) {
					// TODO Auto-generated method stub
					return null;
				}
			});
			Set<CombiMatch> combiMatches = getMatches(this, new HashSet<>(Arrays.asList(r)), false, aMatchConfig);

			Map<BaseRule, Set<Match>> matches = convertToMap(combiMatches);

			if (matches.containsKey(r))
				return matches.get(r);
		}
		return new HashSet<>();
	}

	public Set<Match> antecedentMatches(Set<TriplePattern> aConsequent, EnumSet<MatchFlag> aMatchConfig) {
		if (!this.antecedent.isEmpty()) {
			Rule r = new Rule(new HashSet<>(), aConsequent);
			Set<CombiMatch> combiMatches = getMatches(this, new HashSet<>(Arrays.asList(r)), true, aMatchConfig);
			Map<BaseRule, Set<Match>> matches = convertToMap(combiMatches);
			if (matches.containsKey(r))
				return matches.get(r);
		}
		return new HashSet<>();
	}

	private static List<Match> findMatches(TriplePattern targetTriple, Set<TriplePattern> someCandidateTriplePatterns) {

		assert someCandidateTriplePatterns != null;
		assert targetTriple != null;
		assert !someCandidateTriplePatterns.isEmpty();

		List<Match> matchingTriplePatterns = new ArrayList<>();
		Map<TripleNode, TripleNode> map;
		for (TriplePattern candidateTriple : someCandidateTriplePatterns) {
			map = targetTriple.findMatches(candidateTriple);
			if (map != null) {
				matchingTriplePatterns.add(new Match(candidateTriple, targetTriple, map));
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

	public String getName() {
		return name;
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((antecedent == null) ? 0 : antecedent.hashCode());
		result = prime * result + ((consequent == null) ? 0 : consequent.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
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

	public static Map<TriplePattern, Set<CombiMatch>> getMatchesPerTriplePerRule(Set<TriplePattern> aTargetPattern,
			List<BaseRule> allCandidateRules, boolean useCandidateConsequent) {

		Map<TriplePattern, Set<CombiMatch>> matchesPerRule = new HashMap<>();

		for (BaseRule candidateRule : allCandidateRules) {
			// first find all triples in the consequent that match each triple in the
			// antecedent
			List<Match> foundMatches;
			for (TriplePattern tripleTarget : aTargetPattern) {
				// find all possible matches of the current antecedent triple in the consequent
				if (useCandidateConsequent ? !candidateRule.consequent.isEmpty()
						: !candidateRule.antecedent.isEmpty()) {
					foundMatches = findMatches(tripleTarget,
							useCandidateConsequent ? candidateRule.consequent : candidateRule.antecedent);
					if (!foundMatches.isEmpty()) {

						Set<CombiMatch> ruleMatches = matchesPerRule.get(tripleTarget);
						if (ruleMatches == null) {
							ruleMatches = new HashSet<>();
							matchesPerRule.put(tripleTarget, ruleMatches);
						}

						for (Match m : foundMatches) {
							CombiMatch newCombiMatch = new CombiMatch();
							Set<Match> newMatchSet = new HashSet<>();
							newMatchSet.add(m);
							newCombiMatch.put(candidateRule, newMatchSet);
							ruleMatches.add(newCombiMatch);
						}
					}
				}
			}
		}
		return matchesPerRule;
	}

	// new implementation of matching towards full match

	/**
	 * This method finds matches of {@code someCandidateRules} on
	 * {@code aTargetRule} using the specific {@code aConfig}.
	 * 
	 * @param aTargetRule        The target rule for which we want to find matches.
	 * @param someCandidateRules The candidate rules that we want to check whether
	 *                           and how they match to {@code aTargetRule}.
	 * @param antecedentOfTarget Whether to match the antecedent or consequent of
	 *                           {@code aTargetRule}
	 * @param aConfig            A collection of {@link MatchFlag}s that determine
	 *                           how elaborate the matching process happens.
	 * @return A set of combi matches of {@code someCandidateRules} to
	 *         {@code aTargetRule}
	 */
	public static Set<CombiMatch> getMatches(BaseRule aTargetRule, Set<BaseRule> someCandidateRules,
			boolean antecedentOfTarget, EnumSet<MatchFlag> aConfig) {

		Set<TriplePattern> targetGP = antecedentOfTarget ? aTargetRule.getAntecedent() : aTargetRule.getConsequent();

		/*
		 * we use a list instead of a set for performance reasons. The list does not
		 * call Match#equals(...) method often everytime we add an entry. The algorithm
		 * makes sure matches that are added do not already exist.
		 */
		List<CombiMatch> allMatches = new ArrayList<CombiMatch>();

		// first find all triples in the someCandidateRules that match each triple
		// in targetGP
		Map<TriplePattern, Set<CombiMatch>> combiMatchesPerTriple = getMatchesPerTriplePerRule(targetGP,
				new ArrayList<>(someCandidateRules), antecedentOfTarget);

		// if not every triple pattern can be matched, we stop the process if we require
		// a full match.
		if (targetGP.isEmpty() || (aConfig.contains(MatchFlag.FULLY_COVERED)
				&& combiMatchesPerTriple.keySet().size() < targetGP.size()))
			return new HashSet<>();

		printCombiMatchesPerTriple(aTargetRule, combiMatchesPerTriple);

		List<CombiMatch> biggestMatches = new ArrayList<>();
		List<CombiMatch> smallerMatches = new ArrayList<>();
		List<CombiMatch> toBeAddedToBiggestMatches = null, toBeAddedToSmallerMatches = null;
		Set<Integer> toBeDemotedMatchIndices = null;

		Set<BaseRule> candidateRulesInBiggestMatch = new HashSet<>();

		Iterator<Map.Entry<TriplePattern, Set<CombiMatch>>> triplePatternMatchesIter = combiMatchesPerTriple.entrySet()
				.iterator();

		if (triplePatternMatchesIter.hasNext()) {
			Set<CombiMatch> value = triplePatternMatchesIter.next().getValue();
			LOG.trace("{}/{} ({}): biggest: {}, smaller: {} ({})", 1, combiMatchesPerTriple.size(), value.size(),
					biggestMatches.size(), smallerMatches.size(), aConfig);
			biggestMatches.addAll(value);

			for (CombiMatch cm : biggestMatches) {
				candidateRulesInBiggestMatch.addAll(cm.keySet());
			}
		}

		int cnt = 1;
		// iterate all (except the first) triple patterns of the target graph pattern.
		while (triplePatternMatchesIter.hasNext()) {
			Map.Entry<TriplePattern, Set<CombiMatch>> currentTriplePatternMatches = triplePatternMatchesIter.next();
			LOG.trace("{}/{} ({}): biggest: {}, smaller: {}", ++cnt, combiMatchesPerTriple.size(),
					currentTriplePatternMatches.getValue().size(), biggestMatches.size(), smallerMatches.size());

			Set<CombiMatch> candidateCombiMatches = currentTriplePatternMatches.getValue();

			toBeAddedToBiggestMatches = new ArrayList<>();
			toBeAddedToSmallerMatches = new ArrayList<>();
			toBeDemotedMatchIndices = new HashSet<>();

			// iterate all candidates for current triple pattern
			for (CombiMatch candidateCombiMatch : candidateCombiMatches) {

				boolean candidateWasMerged = false;

				// try to merge with biggest combi matches
				for (int i = 0; i < biggestMatches.size(); i++) {
					CombiMatch aBiggestMatch = biggestMatches.get(i);
					// compare/combine combimatches.

					boolean tryMerge = true;
					// TODO: this does not seem to be going in the right direction, because this way
					// the ordering of the triple patterns is important and we do not want that.
					// TODO can we move this outside the current loops?
					BaseRule candidateRule = candidateCombiMatch.keySet().iterator().next();
					if (aConfig.contains(MatchFlag.ONLY_NEW_RULE_WHEN_NECESSARY)
							/*&& !aBiggestMatch.containsKey(candidateRule)*/ && candidateRulesInBiggestMatch.contains(candidateRule)) {
						// check if a rule already present in aBiggestMatch
						// can match the current triple pattern

						for (CombiMatch cm : candidateCombiMatches) {
							if (!cm.equals(candidateCombiMatch)) {
								if (aBiggestMatch.containsKey(cm.keySet().iterator().next())) {
									tryMerge = false;
								}
							}
						}
					}

					if (tryMerge) {
						CombiMatch newCombiMatch = mergeCombiMatches(candidateCombiMatch, aBiggestMatch, aConfig);

						if (newCombiMatch != null) {
							// successful merge add new biggest and demote old biggest
							toBeAddedToBiggestMatches.add(newCombiMatch);
							candidateWasMerged = true;
							toBeDemotedMatchIndices.add(i);
						} else if (aConfig.contains(MatchFlag.FULLY_COVERED))
							toBeDemotedMatchIndices.add(i);
					}
				}

				if (!aConfig.contains(MatchFlag.FULLY_COVERED)) {

					// we need to sort the smaller matches on size (from big to small)
					// to make sure the isSubCombiMatch method works correctly in this algo

					// do this 'costly' merge operation in parallel
					var newCombiMatches = smallerMatches.stream().parallel().map(aSmallerMatch -> {
						return mergeCombiMatches(candidateCombiMatch, aSmallerMatch, aConfig);
					}).filter(Objects::nonNull).sorted(new CombiMatchSizeComparator()).collect(Collectors.toList());

					// determine where to add new combi matches
					for (CombiMatch newCombiMatch : newCombiMatches) {

						// merge successful, add to smaller matches
						if (candidateWasMerged) {
							if (isSubCombiMatch(newCombiMatch, toBeAddedToBiggestMatches)) {
								toBeAddedToSmallerMatches.add(newCombiMatch);
							} else {
								toBeAddedToBiggestMatches.add(newCombiMatch);
								candidateWasMerged = true;
							}
						} else {
							// add to biggest matches
							candidateWasMerged = true;
							toBeAddedToBiggestMatches.add(newCombiMatch);
						}
					}
				}

				if (!aConfig.contains(MatchFlag.FULLY_COVERED)) {
					if (!candidateWasMerged)
						toBeAddedToBiggestMatches.add(candidateCombiMatch);
					else
						toBeAddedToSmallerMatches.add(candidateCombiMatch);
				}
			}

			// update collections
			List<Integer> sortedList = new ArrayList<>(toBeDemotedMatchIndices);
			Collections.sort(sortedList, Collections.reverseOrder());
			for (int i : sortedList) {

				if (!aConfig.contains(MatchFlag.FULLY_COVERED)) {
					toBeAddedToSmallerMatches.add(biggestMatches.get(i));
				}
				biggestMatches.remove(i);
			}

			// add all toBeAddedMatches
			biggestMatches.addAll(toBeAddedToBiggestMatches);
			smallerMatches.addAll(toBeAddedToSmallerMatches);
			
			for(CombiMatch big: toBeAddedToBiggestMatches)
			{
				candidateRulesInBiggestMatch.addAll(big.keySet());
			}
		}

		toBeAddedToBiggestMatches = null;
		toBeDemotedMatchIndices = null;
		toBeAddedToSmallerMatches = null;

		allMatches.addAll(biggestMatches);

		if (!aConfig.contains(MatchFlag.ONLY_BIGGEST)) {
			allMatches.addAll(smallerMatches);
		}

//		printAllMatches(allMatches);

		return new HashSet<CombiMatch>(allMatches);
	}

	private static void printAllMatches(List<CombiMatch> allMatches) {

		for (CombiMatch cm : allMatches) {
			System.out.println("--------- combi match ------------");
			for (Map.Entry<BaseRule, Set<Match>> entry : cm.entrySet()) {
				BaseRule key = entry.getKey();
				System.out.println(key.getAntecedent() + " -> " + key.getConsequent() + ":");
				for (Match m : entry.getValue()) {
					System.out.println("\t" + m);
				}
			}
		}
	}

	private static boolean isSubCombiMatch(CombiMatch aSmallerMatch, List<CombiMatch> toBeAddedToBiggestMatches) {

		for (CombiMatch combiMatch : toBeAddedToBiggestMatches) {
			if (combiMatch.isSubMatch(aSmallerMatch)) {
				return true;
			}
		}

		return false;
	}

	private static void printCombiMatchesPerTriple(BaseRule aTargetRule,
			Map<TriplePattern, Set<CombiMatch>> combiMatchesPerTriple) {
		StringBuilder sb = new StringBuilder();

		int total = 1;
		for (Set<CombiMatch> combiMatch : combiMatchesPerTriple.values()) {
			total = total * combiMatch.size();
			sb.append(combiMatch.size()).append(" * ");
		}

		LOG.trace("{}: {} = {}", aTargetRule.getName(), total, sb.toString());

	}

	/**
	 * Tries to merge the singleton candidate combi match with the given biggest
	 * match. It takes into account that in some rare cases a single triple pattern
	 * should be allowed to match multiple triple patterns (see {@link CombiMatch}).
	 * 
	 * @param candidateCombiMatch The candidate combi match
	 * @param aBiggestCombiMatch  The biggest match to merge with
	 * @return a new CombiMatch if merge is possible, {@code null} otherwise.
	 */
	private static CombiMatch mergeCombiMatches(CombiMatch candidateCombiMatch, CombiMatch aBiggestCombiMatch,
			EnumSet<MatchFlag> config) {

		assert candidateCombiMatch.keySet().size() == 1;
		Map.Entry<BaseRule, Set<Match>> entry = candidateCombiMatch.entrySet().iterator().next();

		BaseRule candidateRule = entry.getKey();

		assert entry.getValue().size() == 1;
		Match candidateMatch = entry.getValue().iterator().next();

		if (aBiggestCombiMatch.containsKey(candidateRule)) {
			// rule already present, try to merge matches.
			Set<Match> biggestMatch = aBiggestCombiMatch.get(candidateRule);

			Match newMatch;

			CombiMatch newCombiMatch = new CombiMatch(aBiggestCombiMatch);
			Set<Match> newBiggestMatch = new HashSet<>(biggestMatch);

			// we merge it with one of the available matches (does that work?)
			for (Match m : biggestMatch) {
				newMatch = m.merge(candidateMatch);
				if (newMatch != null) {
					// merge successful
					newBiggestMatch.remove(m);
					newBiggestMatch.add(newMatch);
					newCombiMatch.put(candidateRule, newBiggestMatch);
					return newCombiMatch;
				}
			}

			// merge unsuccessful, if special case of same triple matches multiple triples.
			if (!config.contains(MatchFlag.ONE_TO_ONE)
					&& doesSameCandidateTripleMapToDifferentTriple(candidateMatch, biggestMatch)) {
				newBiggestMatch.add(candidateMatch);
				newCombiMatch.put(candidateRule, newBiggestMatch);
				return newCombiMatch;
			}

		} else if (!config.contains(MatchFlag.SINGLE_RULE)) {
			// rule not yet present, add new entry for rule
			CombiMatch newCombiMatch = new CombiMatch(aBiggestCombiMatch);
			Set<Match> matches = new HashSet<>();
			matches.add(candidateMatch);
			newCombiMatch.put(candidateRule, matches);
			return newCombiMatch;
		}
		return null;
	}

	private static boolean doesSameCandidateTripleMapToDifferentTriple(Match candidateMatch,
			Set<Match> biggestMatches) {
		assert candidateMatch.getMatchingPatterns().entrySet().size() == 1;
		var candidateMatchMP = candidateMatch.getMatchingPatterns().entrySet().iterator().next();
		for (Match biggestMatch : biggestMatches) {
			var biggestMatchMP = biggestMatch.getMatchingPatterns();
			for (Map.Entry<TriplePattern, TriplePattern> entry : biggestMatchMP.entrySet()) {
				if (!entry.getValue().equals(candidateMatchMP.getValue())) {
					return true;
				}
			}
		}

		return false;
	}

	private static Map<BaseRule, Set<Match>> convertToMap(Set<CombiMatch> combiMatches) {
		Map<BaseRule, Set<Match>> matchesPerRule = new HashMap<>();
		for (CombiMatch combiMatch : combiMatches) {
			for (Map.Entry<BaseRule, Set<Match>> ruleMatch : combiMatch.entrySet()) {
				// create if not already exists
				if (!matchesPerRule.containsKey(ruleMatch.getKey())) {
					matchesPerRule.put(ruleMatch.getKey(), new HashSet<>());
				}
				assert matchesPerRule.containsKey(ruleMatch.getKey());
				matchesPerRule.get(ruleMatch.getKey()).addAll(ruleMatch.getValue());

			}
		}

		return matchesPerRule;
	}

	/**
	 * This class represents a single combi match which consists of one or more
	 * rules mapped to (in most cases) a single Match object. However, in some
	 * scenario's (like a transitivity rule) we need to store multiple Match objects
	 * for a single rule. Therefore, it maps to a Set of matches. This is caused by
	 * the fact that a Match object does not allow a multiple triple patterns to be
	 * matched to the same triple pattern. This is the case in transitivity
	 * scenario's and we there support multiple match objects.
	 */
	public static class CombiMatch extends HashMap<BaseRule, Set<Match>> {
		private static final long serialVersionUID = 1L;

		public CombiMatch() {
			super();
		}

		public CombiMatch(Map<BaseRule, Set<Match>> someRules) {
			super(someRules);
		}

		/**
		 * @param aMatch a combi match object to check whether it is a sub combi match
		 *               of this combi match.
		 * @return {@code true} when {@code aMatch} is a sub combi match of this combi
		 *         match and {@code false} otherwise.
		 */
		public boolean isSubMatch(CombiMatch aMatch) {

			for (Map.Entry<BaseRule, Set<Match>> entry : aMatch.entrySet()) {

				if (!this.containsKey(entry.getKey())) {
					return false;
				} else if (!isSubSetMatch(entry.getValue(), this.get(entry.getKey()))) {
					return false;
				}
			}

			return true;
		}

		private boolean isSubSetMatch(Set<Match> candidateMatches, Set<Match> targetMatches) {

			for (Match candidateMatch : candidateMatches) {
				boolean found = false;
				for (Match targetMatch : targetMatches) {
					found |= targetMatch.isSubMatch(candidateMatch);
				}

				if (!found) {
					return false;
				}
			}
			return true;
		}

		public CombiMatch(CombiMatch aBiggestCombiMatch) {
			super(aBiggestCombiMatch);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			sb.append("{");
			for (Map.Entry<BaseRule, Set<Match>> entry : this.entrySet()) {
				sb.append(entry.getValue()).append("=").append(entry.getKey());
			}
			sb.append("}");
			return sb.toString();
		}

		public int getSize() {

			Set<TriplePattern> tps = new HashSet<TriplePattern>();
			for (Map.Entry<BaseRule, Set<Match>> entry : this.entrySet()) {

				for (Match m : entry.getValue()) {
					tps.addAll(m.getMatchingPatterns().values());
				}
			}

			return tps.size();
		}
	}
}
