package eu.interconnectproject.knowledge_engine.smartconnector.api;

import org.apache.jena.rdf.model.Resource;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

/**
 * This class provides information about *why* data is being exchanged. So, it
 * should represent a purpose/goal or intent with which the data is being
 * exchanged. Particularly important is whether the {@link KnowledgeInteraction}
 * this {@link CommunicativeAct} belongs to expects side-effects or not.
 */
public class CommunicativeAct {
	private final Resource type;
	
	public CommunicativeAct() {
		this.type = Vocab.COMMUNICATIVE_ACT;
	}

	public CommunicativeAct(Resource aType) {
		this.type = aType;
	}

	public Resource getType() {
		return type;
	}

	public boolean matches(CommunicativeAct other) {
		// In this implementation, a communicative act matches another one iff their
		// types are identical.
		return this.type.equals(other.getType());
	}
}
