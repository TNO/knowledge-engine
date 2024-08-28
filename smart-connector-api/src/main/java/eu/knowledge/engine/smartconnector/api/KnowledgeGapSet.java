package eu.knowledge.engine.smartconnector.api;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A knowledge gap set contains one or more {@link KnowledgeGap}s in an <i><b>OR</b></i> 
 * fashion. This means that these gaps are present when executing an ask or post interaction.
 * 
 * A knowledge gap consists of a set of {@link TriplePattern}s that are missing and need ALL
 * to be present in order to satisfy the ask/post interaction.
 *
 *  */
public class KnowledgeGapSet extends HashSet<KnowledgeGap> {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeGapSet.class);

	private static final long serialVersionUID = 1L;

	public KnowledgeGapSet() {
	}

	/**
	 * Construct a KnowledgeGapSet from a single {@link KnowledgeGap} kg. 
	 */
	public KnowledgeGapSet(KnowledgeGap kg) {
		this.add(kg);
	}

	/**
	 * Write this BindingSet to the standard output.
	 * This is convenient for debugging.
	 */
	public void write() {
		for (KnowledgeGap b : this) {
			System.out.println(b);
		}

	}
}
