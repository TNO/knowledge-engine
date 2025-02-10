package eu.knowledge.engine.reasoner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.graph.Node;

import eu.knowledge.engine.reasoner.api.TripleNode;
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
	private Map<TripleNode, TripleNode> mapping;

	private int hashCodeCache;

	public Match(TriplePattern matchTriple, TriplePattern uponTriple, Map<TripleNode, TripleNode> someMapping) {
		Map<TriplePattern, TriplePattern> someMatchingPatterns = new HashMap<>();
		someMatchingPatterns.put(matchTriple, uponTriple);
		this.matchingPatterns = Collections.unmodifiableMap(someMatchingPatterns);

		Map<TripleNode, TripleNode> newMapping = new HashMap<>();
		newMapping.putAll(someMapping);
		this.mapping = Collections.unmodifiableMap(someMapping);

		this.hashCodeCache = calcHashCode();
	}

	private Match(Map<TriplePattern, TriplePattern> someMatchingPatterns, Map<TripleNode, TripleNode> someMapping) {
		this.matchingPatterns = Collections.unmodifiableMap(someMatchingPatterns);
		this.mapping = Collections.unmodifiableMap(someMapping);
		this.hashCodeCache = calcHashCode();
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

		var mergedMatchingPatterns = mergeMatchingPatterns(this.getMatchingPatterns(),
				otherMatch.getMatchingPatterns());

		// if both sides of the matching patterns do not overlap
		if (mergedMatchingPatterns != null) {
			// and if the mappings do not conflict
			Map<TripleNode, TripleNode> mergedMapping = mergeContexts(this.getMappings(), otherMatch.getMappings());
			if (mergedMapping != null) {
				// if both patterns and mappings do not conflict.
				m = new Match(mergedMatchingPatterns, mergedMapping);
			}
		}
		return m;
	}

	/**
	 * Checks both the keys and the values.
	 * 
	 * @param aFirstMap
	 * @param aSecondMap
	 * @return {@code null} if the matching patterns overlap on either keys or
	 *         values, otherwise the merged mapping.
	 */
	private HashMap<TriplePattern, TriplePattern> mergeMatchingPatterns(Map<TriplePattern, TriplePattern> aFirstMap,
			Map<TriplePattern, TriplePattern> aSecondMap) {

		var mergedMatchingPatterns = new HashMap<TriplePattern, TriplePattern>(aFirstMap.size() + aSecondMap.size());

		boolean firstTime = true;

		for (Entry<TriplePattern, TriplePattern> entry1 : aFirstMap.entrySet()) {
			for (Entry<TriplePattern, TriplePattern> entry2 : aSecondMap.entrySet()) {
				if (entry1.getKey().equals(entry2.getKey()) || entry1.getValue().equals(entry2.getValue()))
					return null;

				if (firstTime)
					mergedMatchingPatterns.put(entry2.getKey(), entry2.getValue());
			}
			mergedMatchingPatterns.put(entry1.getKey(), entry1.getValue());
			firstTime = false;
		}

		return mergedMatchingPatterns;
	}

	public Map<TripleNode, TripleNode> getMappings() {
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
	private Map<TripleNode, TripleNode> mergeContexts(Map<TripleNode, TripleNode> existingContext,
			Map<TripleNode, TripleNode> newContext) {

		Collection<TripleNode> existingContextValues = existingContext.values();
		Map<TripleNode, TripleNode> mergedContext = new HashMap<TripleNode, TripleNode>(
				existingContext.size() + newContext.size());
		mergedContext.putAll(existingContext);

		for (Map.Entry<TripleNode, TripleNode> newEntry : newContext.entrySet()) {
			Node node;
			if ((node = getOtherNode(existingContext, newEntry.getKey().node)) != null) {
				if (!node.equals(newEntry.getValue().node)) {
					return null;
				}
			} else {

				for (TripleNode tn : existingContextValues) {
					if (tn.node.equals(newEntry.getValue().node)) {
						return null;
					}
				}
				// no conflict, so we can safely put it into the mergedContext.
				mergedContext.put(newEntry.getKey(), newEntry.getValue());

			}
		}

		return mergedContext;
	}

	public Node getOtherNode(Map<TripleNode, TripleNode> aContext, Node aNode) {
		for (Map.Entry<TripleNode, TripleNode> entry : aContext.entrySet()) {
			if (entry.getKey().node.equals(aNode)) {
				return entry.getValue().node;
			}
		}
		return null;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Map.Entry<TriplePattern, TriplePattern> entry : this.matchingPatterns.entrySet()) {

			boolean firstTriplePattern = true;
			for (TriplePattern tp : new TriplePattern[] { entry.getKey(), entry.getValue() }) {
				boolean firstTime = true;
				Node[] nodes = new Node[] { tp.getSubject(), tp.getPredicate(), tp.getObject() };
				for (int i = 0; i < 3; i++) {
					Node n = nodes[i];

					if (!firstTime) {
						sb.append(" ");
					}
					var truncatedNode = TriplePattern.trunc(n);

					var tn = new TripleNode(tp, n, i);

					if (firstTriplePattern ? this.mapping.containsValue(tn) : this.mapping.containsKey(tn)) {
						sb.append("|").append(truncatedNode).append("|");
					} else {
						sb.append(truncatedNode);
					}
					firstTime = false;
				}
				if (firstTriplePattern)
					sb.append("=");
				firstTriplePattern = false;
			}
			sb.append(", ");
		}
		sb.deleteCharAt(sb.length() - 1).deleteCharAt(sb.length() - 1).append("}");

		return "Match " + sb.toString();
	}

	/**
	 * invert the triple patterns and mapping.
	 * 
	 * @return
	 */
	public Match inverse() {
		Map<TripleNode, TripleNode> invertedMap = new HashMap<TripleNode, TripleNode>();
		Map<TriplePattern, TriplePattern> newMatchingPatterns = new HashMap<TriplePattern, TriplePattern>();

		for (Map.Entry<TriplePattern, TriplePattern> someMatchingPatterns : this.matchingPatterns.entrySet()) {
			newMatchingPatterns.put(someMatchingPatterns.getValue(), someMatchingPatterns.getKey());
		}

		for (Map.Entry<TripleNode, TripleNode> entry : this.getMappings().entrySet()) {
			invertedMap.put(entry.getValue(), entry.getKey());
		}
		return new Match(newMatchingPatterns, invertedMap);
	}

	public static Set<Match> invertAll(Set<Match> someMatches) {
		Set<Match> inverseMatches = new HashSet<>();
		for (Match m : someMatches) {
			inverseMatches.add(m.inverse());
		}
		return inverseMatches;
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

		for (Entry<TripleNode, TripleNode> entry : aMatch.getMappings().entrySet()) {
			if (!entry.getValue().equals(this.mapping.get(entry.getKey()))) {
				return false;
			}
		}

		return true;
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((matchingPatterns == null) ? 0 : matchingPatterns.hashCode());
		return result;
	}

	@Override
	public int hashCode() {
		return this.hashCodeCache;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Match other = (Match) obj;
		if (matchingPatterns == null) {
			if (other.matchingPatterns != null)
				return false;
		} else if (!matchingPatterns.equals(other.matchingPatterns))
			return false;
		return true;
	}
}