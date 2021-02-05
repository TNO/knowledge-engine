package interconnect.ke.sc;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class Vocab {

	public static final String ONTO_URI = "https://www.tno.nl/energy/ontology/interconnect#";

	public static final Resource KNOWLEDGE_BASE = ResourceFactory.createResource(ONTO_URI + "KnowledgeBase");
	public static final Resource KNOWLEDGE_INTERACTION = ResourceFactory
			.createResource(ONTO_URI + "KnowledgeInteraction");
	public static final Resource REACT_KI = ResourceFactory.createResource(ONTO_URI + "ReactKnowledgeInteraction");
	public static final Resource POST_KI = ResourceFactory.createResource(ONTO_URI + "PostKnowledgeInteraction");
	public static final Resource ANSWER_KI = ResourceFactory.createResource(ONTO_URI + "AnswerKnowledgeInteraction");
	public static final Resource ASK_KI = ResourceFactory.createResource(ONTO_URI + "AskKnowledgeInteraction");

	public static final Property HAS_GP = ResourceFactory.createProperty(ONTO_URI + "hasGraphPattern");
	public static final Property HAS_NAME = ResourceFactory.createProperty(ONTO_URI + "hasName");
	public static final Property HAS_DESCR = ResourceFactory.createProperty(ONTO_URI + "hasDescription");
	public static final Property HAS_KI = ResourceFactory.createProperty(ONTO_URI + "hasKnowledgeInteraction");
	public static final Property IS_META = ResourceFactory.createProperty(ONTO_URI + "isMeta");
	public static final Property HAS_PATTERN = ResourceFactory.createProperty(ONTO_URI + "hasPattern");
	public static final Property HAS_ARG = ResourceFactory.createProperty(ONTO_URI + "hasArgumentGraphPattern");
	public static final Property HAS_RES = ResourceFactory.createProperty(ONTO_URI + "hasResultGraphPattern");
	
	public static final Resource COMMUNICATIVE_ACT = ResourceFactory.createProperty(ONTO_URI + "CommunicativeAct");
	public static final Resource CONVERSION_ACT = ResourceFactory.createProperty(ONTO_URI + "ConversionAct");
	public static final Resource NEW_KNOWLEDGE_ACT = ResourceFactory.createProperty(ONTO_URI + "NewKnowledgeAct");
	public static final Resource RETRIEVE_KNOWLEDGE_ACT = ResourceFactory.createProperty(ONTO_URI + "RetrieveKnowledgeAct");
	public static final Resource CHANGED_KNOWLEDGE_ACT = ResourceFactory.createProperty(ONTO_URI + "ChangedKnowledgeAct");
	public static final Resource REMOVED_KNOWLEDGE_ACT = ResourceFactory.createProperty(ONTO_URI + "RemovedKnowledgeAct");
	public static final Resource PERIODIC_ACT = ResourceFactory.createProperty(ONTO_URI + "PeriodicAct");
	public static final Resource WHEN_CHANGED_ACT = ResourceFactory.createProperty(ONTO_URI + "WhenChangedAct");
}
