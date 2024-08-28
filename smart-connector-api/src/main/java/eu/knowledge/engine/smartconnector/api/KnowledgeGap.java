package eu.knowledge.engine.smartconnector.api;

import java.util.HashSet;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TriplePattern;

/**
 * A knowledge gap consists of a set of {@link TriplePattern}s that are missing and need ALL
 * to be present in order to satisfy a ask/post interaction.
 *
 * Note that there can be multiple knowledge gaps in an ask or post interaction.
 * They will be combined in a set of knowledge gaps.
 */
public class KnowledgeGap extends HashSet<TriplePattern> {

	private static final long serialVersionUID = 1L;
	
	public KnowledgeGap() {
	}

	/**
	 * Construct a KnowledgeGap from a set of TriplePatterns tps
	 */
	public KnowledgeGap(Set<TriplePattern> tps) {
		for (TriplePattern tp: tps) {
			this.add(tp);
		}
	}

}
