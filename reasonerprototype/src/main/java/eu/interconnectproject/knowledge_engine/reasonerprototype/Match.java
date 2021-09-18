package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.Map;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

/**
 * A match between two graph patterns. It can be full, partial. There is the gp
 * that is being matched and the gp onto which the other is matched.
 * 
 * @author nouwtb
 *
 */
public class Match {

	private Set<Triple> toBeMatched;
	private Set<Triple> matchedUpon;

	/**
	 * Could be partial matches (i.e. only a single triple from toBeMatched matches
	 * to the matchedUpon)
	 */
	private Set<Map<Triple, Triple>> possibleMatches;

}
