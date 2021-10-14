package eu.knowledge.engine.reasonerprototype;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Value;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

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
			public BindingSet handle(BindingSet bs) {

				BindingSet newBS = new BindingSet();

				Binding newB;

				Set<Variable> vars = Rule.this.getVars(Rule.this.consequent);
				for (Binding b : bs) {
					newB = new Binding();
					for (Variable v : vars) {
						if (b.containsKey(v)) {
							newB.put(v, b.get(v));
						} else {
							System.err.println(
									"Not all variable in the consequent are available in the antecedent of the rule. This type of rule should use a custom BindingHandler.");
						}
					}
					newBS.add(newB);
				}

				return newBS;
			}
		};
	}

	public Set<Variable> getVars(Set<TriplePattern> aPattern) {
		Set<Variable> vars = new HashSet<Variable>();
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
	 * FInd the biggest matches bewteen two graph patterns.
	 * 
	 * @param aFirstPattern
	 * @param aSecondPattern
	 * @param aMatchStrategy
	 * @return
	 */
	private Set<Match> matches(Set<TriplePattern> aFirstPattern, Set<TriplePattern> aSecondPattern,
			MatchStrategy aMatchStrategy) {

		assert aFirstPattern != null;
		assert aSecondPattern != null;
		assert !aFirstPattern.isEmpty();
		assert !aSecondPattern.isEmpty();

		long start = System.currentTimeMillis();

		// first find all triples in the consequent that match each triple in the
		// antecedent
		List<Match> allMatches = new LinkedList<>();

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
			return new HashSet<>(allMatches);

		// next, correctly combine all found matches
		Match mergedMatch = null;
		Set<Match> matches = null;
		List<Match> newMatches = null, removeMatches = null;

		Iterator<Map.Entry<TriplePattern, Set<Match>>> matchIter = matchesPerTriple.entrySet().iterator();

		// always add first matches
		if (matchIter.hasNext()) {
			allMatches.addAll(matchIter.next().getValue());
		}

		while (matchIter.hasNext()) {
			Map.Entry<TriplePattern, Set<Match>> entry = matchIter.next();

			// keep a set of new matches, so we can add them at the end of this loop
			newMatches = new LinkedList<>();
			removeMatches = new LinkedList<>();

			matches = entry.getValue();
			assert matches != null;

			for (Match m1 : matches) {
				// check if we need to merge with existing matches

				for (Match m2 : allMatches) {
					mergedMatch = m2.merge(m1);
					if (mergedMatch != null) {
						newMatches.add(mergedMatch);
						removeMatches.add(m2);
					}
				}
			}

			if (aMatchStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)
					|| (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES) && newMatches.isEmpty())) {
				newMatches.addAll(matches);
			}

			if (!aMatchStrategy.equals(MatchStrategy.FIND_ALL_MATCHES))
				allMatches.removeAll(removeMatches);

			allMatches.addAll(newMatches);

			long end = System.currentTimeMillis();
			newMatches = null;

		}
		assert allMatches != null;

		long finalEnd = System.currentTimeMillis();

		return new HashSet<>(allMatches);
	}

	private Set<Match> findMatches(TriplePattern antecedent, Set<TriplePattern> consequent) {

		assert consequent != null;
		assert antecedent != null;
		assert !consequent.isEmpty();

		Set<Match> matchingTriplePatterns = new HashSet<>();
		Map<Value, Value> map;
		for (TriplePattern tp : consequent) {
			map = antecedent.findMatches(tp);
			if (map != null) {
				matchingTriplePatterns.add(new Match(antecedent, tp, map));
			}
		}

		assert matchingTriplePatterns != null;
		return matchingTriplePatterns;
	}

	@Override
	public String toString() {
		return "Rule [antecedent=" + antecedent + ", consequent=" + consequent + "]";
	}

	public Set<Variable> getVars() {

		Set<Variable> vars = new HashSet<>();
		;
		if (this.antecedent != null)
			vars.addAll(this.getVars(this.antecedent));

		if (this.consequent != null)
			vars.addAll(this.getVars(this.consequent));

		return vars;
	}

	/**
	 * Represents a single match between two graph patterns.
	 *
	 * It contains which triple matches upon which triple and it contains the
	 * required mapping from variables to literals and vice versa to realize the
	 * match.
	 * 
	 * Note that we assume that this match is only used within a single matching
	 * process between two graph patterns. These matches should not be transferred
	 * in other matching processes.
	 *
	 * @author nouwtb
	 *
	 */
	public static class Match {

		/**
		 * keys: things that are matching, values: things upon which they match
		 */
		private Map<TriplePattern, TriplePattern> matchingPatterns;

		/**
		 * keys: thing that is mapped, values: thing upon which it maps
		 * 
		 * Note that the semantics of this mapping is that it contains all mappings that
		 * involve variables. Literal to literal mappings are left out.
		 */
		private Map<Value, Value> mapping;

		public Match(TriplePattern matchTriple, TriplePattern uponTriple, Map<Value, Value> someMapping) {
			Map<TriplePattern, TriplePattern> someMatchingPatterns = new HashMap<>();
			someMatchingPatterns.put(matchTriple, uponTriple);
			this.matchingPatterns = Collections.unmodifiableMap(someMatchingPatterns);

			Map<Value, Value> newMapping = new HashMap<>();
			newMapping.putAll(someMapping);
			this.mapping = Collections.unmodifiableMap(someMapping);

		}

		private Match(Map<TriplePattern, TriplePattern> someMatchingPatterns, Map<Value, Value> someMapping) {
			this.matchingPatterns = Collections.unmodifiableMap(someMatchingPatterns);
			this.mapping = Collections.unmodifiableMap(someMapping);
		}

		/**
		 * Only merges if they are not conflicting. Conflicts arise if the mapping
		 * conflicts or the matching patterns conflict (i.e. matching multiple times to
		 * or from the same triple)
		 * 
		 * @param otherMatch
		 * @return a new consistently merged match, otherwise {@code null}.
		 */
		public Match merge(Match otherMatch) {

			Match m = null;

			// if both sides of the matching patterns do not overlap
			boolean doKeysIntersect = doIntersect(this.getMatchingPatterns().keySet(),
					otherMatch.getMatchingPatterns().keySet());

			boolean doValuesIntersect = doIntersect(this.getMatchingPatterns().values(),
					otherMatch.getMatchingPatterns().values());

			if (!doKeysIntersect && !doValuesIntersect) {

				// and if the mappings do not conflict
				Map<Value, Value> mergedMapping = mergeContexts(this.getMappings(), otherMatch.getMappings());
				if (mergedMapping != null) {
					// if both patterns and mappings do not conflict.
					Map<TriplePattern, TriplePattern> newMatchingPatterns = new HashMap<>(this.getMatchingPatterns());
					newMatchingPatterns.putAll(otherMatch.getMatchingPatterns());

					Map<Value, Value> newMapping = new HashMap<>(this.getMappings());
					newMapping.putAll(otherMatch.getMappings());
					m = new Match(newMatchingPatterns, newMapping);
				}
			}
			return m;
		}

		private boolean doIntersect(Collection<TriplePattern> aFirstSet, Collection<TriplePattern> aSecondSet) {

			for (TriplePattern tp1 : aFirstSet) {
				for (TriplePattern tp2 : aSecondSet) {
					if (tp1.equals(tp2))
						return true;
				}
			}

			return false;
		}

		public Map<Value, Value> getMappings() {
			return this.mapping;
		}

		public Map<TriplePattern, TriplePattern> getMatchingPatterns() {
			return this.matchingPatterns;
		}

		/**
		 * Returns null if the contexts have conflicting values.
		 * 
		 * @param existingContext
		 * @param newContext
		 * @return
		 */
		private Map<Value, Value> mergeContexts(Map<Value, Value> existingContext, Map<Value, Value> newContext) {

			Map<Value, Value> mergedContext = new HashMap<Value, Value>(existingContext);
			for (Map.Entry<Value, Value> newEntry : newContext.entrySet()) {
				if (existingContext.containsKey(newEntry.getKey())) {
					if (!existingContext.get(newEntry.getKey()).equals(newEntry.getValue())) {
						return null;
					}
				} else if (existingContext.values().contains(newEntry.getValue())) {
					return null;
				} else {
					mergedContext.put(newEntry.getKey(), newEntry.getValue());
				}
			}

			return mergedContext;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((mapping == null) ? 0 : mapping.hashCode());
			result = prime * result + ((matchingPatterns == null) ? 0 : matchingPatterns.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Match)) {
				return false;
			}
			Match other = (Match) obj;
			if (mapping == null) {
				if (other.mapping != null) {
					return false;
				}
			} else if (!mapping.equals(other.mapping)) {
				return false;
			}
			if (matchingPatterns == null) {
				if (other.matchingPatterns != null) {
					return false;
				}
			} else if (!matchingPatterns.equals(other.matchingPatterns)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "Match [matchingPatterns=" + matchingPatterns + ", mapping=" + mapping + "]";
		}

		/**
		 * invert the triple patterns and mapping.
		 * 
		 * @return
		 */
		public Match inverse() {
			Map<Value, Value> invertedMap = new HashMap<Value, Value>();
			Map<TriplePattern, TriplePattern> newMatchingPatterns = new HashMap<TriplePattern, TriplePattern>();

			for (Map.Entry<TriplePattern, TriplePattern> someMatchingPatterns : this.matchingPatterns.entrySet()) {
				newMatchingPatterns.put(someMatchingPatterns.getValue(), someMatchingPatterns.getKey());
			}

			for (Map.Entry<Value, Value> entry : this.getMappings().entrySet()) {
				invertedMap.put(entry.getValue(), entry.getKey());
			}
			return new Match(newMatchingPatterns, invertedMap);
		}
	}

}
