package eu.knowledge.engine.reasoner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.graph.Node;

import eu.knowledge.engine.reasoner.api.TriplePattern;

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
public class Match {

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
	private Map<Node, Node> mapping;

	public Match(TriplePattern matchTriple, TriplePattern uponTriple, Map<Node, Node> someMapping) {
		Map<TriplePattern, TriplePattern> someMatchingPatterns = new HashMap<>();
		someMatchingPatterns.put(matchTriple, uponTriple);
		this.matchingPatterns = Collections.unmodifiableMap(someMatchingPatterns);

		Map<Node, Node> newMapping = new HashMap<>();
		newMapping.putAll(someMapping);
		this.mapping = Collections.unmodifiableMap(someMapping);
	}

	private Match(Map<TriplePattern, TriplePattern> someMatchingPatterns, Map<Node, Node> someMapping) {
		this.matchingPatterns = Collections.unmodifiableMap(someMatchingPatterns);
		this.mapping = Collections.unmodifiableMap(someMapping);
	}

	/**
	 * Only merges if they are not conflicting. Conflicts arise if the mapping
	 * conflicts or the matching patterns conflict (i.e. matching multiple times to
	 * or from the same triple).
	 * 
	 * Note that merge is symmetric: {@code m1.merge(m2).equals(m2.merge(m1))}
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
			Map<Node, Node> mergedMapping = mergeContexts(this.getMappings(), otherMatch.getMappings());
			if (mergedMapping != null) {
				// if both patterns and mappings do not conflict.
				Map<TriplePattern, TriplePattern> newMatchingPatterns = new HashMap<>(this.getMatchingPatterns());
				newMatchingPatterns.putAll(otherMatch.getMatchingPatterns());

				Map<Node, Node> newMapping = new HashMap<>(this.getMappings());
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

	public Map<Node, Node> getMappings() {
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
	private Map<Node, Node> mergeContexts(Map<Node, Node> existingContext, Map<Node, Node> newContext) {

		Map<Node, Node> mergedContext = new HashMap<Node, Node>(existingContext);
		for (Map.Entry<Node, Node> newEntry : newContext.entrySet()) {
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

	/**
	 * invert the triple patterns and mapping.
	 * 
	 * @return
	 */
	public Match inverse() {
		Map<Node, Node> invertedMap = new HashMap<Node, Node>();
		Map<TriplePattern, TriplePattern> newMatchingPatterns = new HashMap<TriplePattern, TriplePattern>();

		for (Map.Entry<TriplePattern, TriplePattern> someMatchingPatterns : this.matchingPatterns.entrySet()) {
			newMatchingPatterns.put(someMatchingPatterns.getValue(), someMatchingPatterns.getKey());
		}

		for (Map.Entry<Node, Node> entry : this.getMappings().entrySet()) {
			invertedMap.put(entry.getValue(), entry.getKey());
		}
		return new Match(newMatchingPatterns, invertedMap);
	}

	/**
	 * Checks if aMatch is a submatch of this match.
	 * 
	 * @param aMatch
	 * @return
	 */
	public boolean isSubMatch(Match aMatch) {

		for (Entry<TriplePattern, TriplePattern> entry : aMatch.getMatchingPatterns().entrySet()) {

			if (!entry.getValue().equals(this.matchingPatterns.get(entry.getKey()))) {
				return false;
			}
		}

		for (Entry<Node, Node> entry : aMatch.getMappings().entrySet()) {
			if (!entry.getValue().equals(this.mapping.get(entry.getKey()))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return "Match [matchingPatterns=" + matchingPatterns + "]";
	}
}