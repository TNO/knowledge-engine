package eu.knowledge.engine.reasonerprototype;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasonerprototype.RuleAlt.Match;
import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Value;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

public class RuleAlt {

	public Set<TriplePattern> antecedent;
	public Set<TriplePattern> consequent;

	public BindingSetHandler bindingSetHandler;

	public RuleAlt(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent,
			BindingSetHandler aBindingSetHandler) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = aBindingSetHandler;
	}

	public RuleAlt(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = new BindingSetHandler() {
			@Override
			public BindingSet handle(BindingSet bs) {

				BindingSet newBS = new BindingSet();

				Binding newB;

				Set<Variable> vars = RuleAlt.this.getVars(RuleAlt.this.consequent);
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
			vars.addAll(t.getVars());
		}
		return vars;
	}

	public Set<Map<TriplePattern, TriplePattern>> consequentMatches(Set<TriplePattern> objective) {
		Set<Map<TriplePattern, TriplePattern>> matches = matches(objective, consequent, new HashMap<>());
		return matches;
	}

	public Set<Match> consequentMatches2(Set<TriplePattern> antecedent, boolean aFullMatchOnly) {
		Set<Match> matches = matches2(antecedent, consequent, aFullMatchOnly);
		return matches;
	}

	public BindingSetHandler getBindingSetHandler() {
		return bindingSetHandler;
	}

	private Set<Match> matches2(Set<TriplePattern> antecedent, Set<TriplePattern> consequent, boolean fullMatchOnly) {

		assert antecedent != null;
		assert consequent != null;
		assert !antecedent.isEmpty();
		assert !consequent.isEmpty();

		// first find all triples in the consequent that match each triple in the
		// antecedent
		Set<Match> allMatches = new HashSet<>();

		Map<TriplePattern, Set<Match>> matchesPerTriple = new HashMap<>();
		for (TriplePattern anteTriple : antecedent) {
			// find all possible matches of the current antecedent triple in the consequent
			matchesPerTriple.put(anteTriple, findMatches(anteTriple, consequent));
		}

		// if not every triple pattern can be matched, we stop the process if we require
		// a full match.
		if (fullMatchOnly && matchesPerTriple.keySet().size() < antecedent.size())
			return allMatches;

		// next, correctly combine all found matches
		Match mergedMatch = null;
		Set<Match> matches = null, newMatches = null;
		boolean firstTime = true;
		for (Map.Entry<TriplePattern, Set<Match>> entry : matchesPerTriple.entrySet()) {

			// keep a set of new matches, so we can add them at the end of this loop
			newMatches = new HashSet<>();

			matches = entry.getValue();
			assert matches != null;

			if (firstTime) {
				// first time there is nothing there.
				newMatches.addAll(matches);
				firstTime = false;
			} else {
				for (Match m1 : matches) {
					// check if we need to merge with existing matches
					for (Match m2 : allMatches) {
						mergedMatch = m1.merge(m2);
						if (mergedMatch != null) {
							newMatches.add(mergedMatch);
						}
					}
				}

				if (!fullMatchOnly && newMatches.isEmpty()) {
					// we only add the individual ones, if we could not add them to existing ones.
					newMatches.addAll(matches);
				}
			}

			if (fullMatchOnly) {
				allMatches = newMatches;
			} else {
				allMatches.addAll(newMatches);
			}
			newMatches = null;

		}

		assert allMatches != null;
		System.out.println("All matches: " + allMatches.size());
		return allMatches;

	}

	/**
	 * 
	 * We match every triple from the objective with every triple of the consequent.
	 * 
	 * TODO It's recursive and uses the Java stack. There is a limit to the stack
	 * size in Java, so this limits the sizes of the graph patterns. We considered
	 * using a loop instead of recursing, but we could not figure out how to do it.
	 * Also, this method is far from efficient, we need to figure out a way to
	 * improve its performance. Maybe using the dynamic programming algorithm
	 * described via here: {@see <a href=
	 * "https://stackoverflow.com/questions/32163716/partial-string-matching-in-two-large-strings">https://stackoverflow.com/questions/32163716/partial-string-matching-in-two-large-strings</a>}.
	 * 
	 * @param objective
	 * @param binding
	 */
	private Set<Map<TriplePattern, TriplePattern>> matches(Set<TriplePattern> objective, Set<TriplePattern> consequent,
			Map<Value, Value> context) {
		Set<Map<TriplePattern, TriplePattern>> isos = new HashSet<>();
		for (TriplePattern objTriple : objective) {
			Map<TriplePattern, Map<Value, Value>> allMatches = rhsMatchesAlt(objTriple, consequent);

			Set<TriplePattern> reducedObj = new HashSet<>(objective);
			reducedObj.remove(objTriple);
			Set<TriplePattern> reducedRhs;
			for (Map.Entry<TriplePattern, Map<Value, Value>> entry : allMatches.entrySet()) {

				// add singleton match
				Map<TriplePattern, TriplePattern> singletonMatch = new HashMap<TriplePattern, TriplePattern>();
				singletonMatch.put(objTriple, entry.getKey());
				isos.add(singletonMatch);

				reducedRhs = new HashSet<>(consequent);
				reducedRhs.remove(entry.getKey());
				Map<Value, Value> newContext = entry.getValue();
				Map<Value, Value> mergedContext;

				if ((mergedContext = mergeContexts(context, newContext)) != null) {

					Set<Map<TriplePattern, TriplePattern>> otherIsos = new HashSet<>();
					if (!reducedObj.isEmpty()) {

						otherIsos = matches(reducedObj, reducedRhs, mergedContext);

						if (!otherIsos.isEmpty()) {
							for (Map<TriplePattern, TriplePattern> iso : otherIsos) {
								iso.put(objTriple, entry.getKey());
							}
						} else {
							Map<TriplePattern, TriplePattern> otherIso = new HashMap<TriplePattern, TriplePattern>();
							otherIso.put(objTriple, entry.getKey());
							otherIsos.add(otherIso);
						}
					} else {
						Map<TriplePattern, TriplePattern> otherIso = new HashMap<TriplePattern, TriplePattern>();
						otherIso.put(objTriple, entry.getKey());
						otherIsos.add(otherIso);
					}
					isos.addAll(otherIsos);
				}
			}
		}
		return isos;
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
			} else {
				mergedContext.put(newEntry.getKey(), newEntry.getValue());
			}
		}

		return mergedContext;
	}

	private Map<TriplePattern, Map<Value, Value>> rhsMatchesAlt(TriplePattern objTriple, Set<TriplePattern> rhs) {
		Map<TriplePattern, Map<Value, Value>> allMatches = new HashMap<>();
		for (TriplePattern myTriple : rhs) {
			Map<Value, Value> map = myTriple.matchesWithSubstitutionMap(objTriple);

			if (map != null && !map.isEmpty())
				allMatches.put(myTriple, map);
		}
		return allMatches;
	}

	private Set<Match> findMatches(TriplePattern antecedent, Set<TriplePattern> consequent) {

		assert consequent != null;
		assert antecedent != null;
		assert !consequent.isEmpty();

		Set<Match> matchingTriplePatterns = new HashSet<>();
		Map<Value, Value> map;
		for (TriplePattern tp : consequent) {
			map = antecedent.matchesWithSubstitutionMap(tp);
			if (map != null) {
				matchingTriplePatterns.add(new Match(antecedent, tp, map));
			}
		}

		assert matchingTriplePatterns != null;
		return matchingTriplePatterns;
	}

	@Override
	public String toString() {
		return "RuleAlt [antecedent=" + antecedent + ", consequent=" + consequent + "]";
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
		private final Map<TriplePattern, TriplePattern> matchingPatterns;

		/**
		 * keys: thing that is mapped, values: thing upon which it maps
		 * 
		 * Note that the semantics of this mapping is that it contains all mappings that
		 * involve variables. Literal to literal mappings are left out.
		 */
		private final Map<Value, Value> mapping;

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
