package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

/**
 * This class provides information about *why* data is being exchanged. So, it
 * should represent a purpose with which the data is being exchanged.
 * Particularly important is whether the {@link KnowledgeInteraction} this
 * {@link CommunicativeAct} belongs to expects side-effects or not.
 * 
 * The communicative act differentiates between the purposes a particular
 * KnowledgeInteraction requires and the purposes it satisfies itself.
 * 
 * The communicative act is used by the Knowledge Engine when searching for
 * matching Knowledge Interactions from other Knowledge Bases. Not only the
 * graph patterns need to match, but also the communicative act. Two
 * communicative acts match if the requirement purposes of the one are a subset
 * of the staisfaction purposes of the other and vice versa (i.e. the
 * requirement purposes of the other are a subset of the satisfaction purposes
 * of this communicative act).
 */
public class CommunicativeAct {

	private final Set<Resource> requirementPurposes;
	private final Set<Resource> satisfactionPurposes;

	public CommunicativeAct() {
		this(new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE)), new HashSet<Resource>(Arrays.asList(Vocab.INFORM_PURPOSE)));
	}

	/**
	 * Intialize this Communicative Act with the given purposes. Note that the sets
	 * cannot be empty.
	 * 
	 * @param someRequirementPurposes  The purposes this communicativeact requires
	 *                                 from the other communicative act. Note that
	 *                                 the purposes are {@link Resource}s that
	 *                                 represent subclasses of the Purpose class in
	 *                                 the ontology.
	 * @param someSatisfactionPurposes The purposes this communicativeact satisfies
	 *                                 of the other communicative act. Note that the
	 *                                 purposes are {@link Resource}s that represent
	 *                                 subclasses of the Purpose class in the
	 *                                 ontology.
	 */
	public CommunicativeAct(Set<Resource> someRequirementPurposes, Set<Resource> someSatisfactionPurposes) {

		if (someRequirementPurposes.isEmpty())
			throw new IllegalArgumentException("There should be at least 1 requirement purpose.");
		if (someSatisfactionPurposes.isEmpty())
			throw new IllegalArgumentException("There should be at least 1 satisfaction purpose.");

		requirementPurposes = someRequirementPurposes;
		satisfactionPurposes = someSatisfactionPurposes;
	}

	public Set<Resource> getRequirementPurposes() {
		return Collections.unmodifiableSet(requirementPurposes);
	}

	public Set<Resource> getSatisfactionPurposes() {
		return Collections.unmodifiableSet(satisfactionPurposes);
	}
}
