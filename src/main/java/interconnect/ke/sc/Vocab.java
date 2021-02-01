package interconnect.ke.sc;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class Vocab {

	public static final String ONTO_URI = "https://www.tno.nl/energy/ontology/interconnect#";

	static final Resource KNOWLEDGE_BASE = ResourceFactory.createResource(ONTO_URI + "KnowledgeBase");
	static final Resource KNOWLEDGE_INTERACTION = ResourceFactory.createResource(ONTO_URI + "KnowledgeInteraction");
	static final Resource REACT_KI = ResourceFactory.createResource(ONTO_URI + "ReactKnowledgeInteraction");
	static final Resource POST_KI = ResourceFactory.createResource(ONTO_URI + "PostKnowledgeInteraction");
	static final Resource ANSWER_KI = ResourceFactory.createResource(ONTO_URI + "AnswerKnowledgeInteraction");
	static final Resource ASK_KI = ResourceFactory.createResource(ONTO_URI + "AskKnowledgeInteraction");

	static final Property HAS_GP = ResourceFactory.createProperty(ONTO_URI + "hasGraphPattern");
	static final Property HAS_NAME = ResourceFactory.createProperty(ONTO_URI + "hasName");
	static final Property HAS_DESCR = ResourceFactory.createProperty(ONTO_URI + "hasDescription");
	static final Property HAS_KI = ResourceFactory.createProperty(ONTO_URI + "hasKnowledgeInteraction");
	static final Property IS_META = ResourceFactory.createProperty(ONTO_URI + "isMeta");
	static final Property HAS_PATTERN = ResourceFactory.createProperty(ONTO_URI + "hasPattern");
	static final Property HAS_ARG = ResourceFactory.createProperty(ONTO_URI + "hasArgumentGraphPattern");
	static final Property HAS_RES = ResourceFactory.createProperty(ONTO_URI + "hasResultGraphPattern");

}
