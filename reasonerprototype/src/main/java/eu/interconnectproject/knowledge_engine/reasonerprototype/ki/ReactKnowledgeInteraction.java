package eu.interconnectproject.knowledge_engine.reasonerprototype.ki;

import java.net.URI;
import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public abstract class ReactKnowledgeInteraction extends KnowledgeInteraction {

	private final List<Triple> argumentGraphPattern;
	private final List<Triple> resultGraphPattern;

	public ReactKnowledgeInteraction(URI id, List<Triple> argumentGraphPattern, List<Triple> resultGraphPattern) {
		super(id);
		this.argumentGraphPattern = argumentGraphPattern;
		this.resultGraphPattern = resultGraphPattern;
	}

	public List<Triple> getArgumentGraphPattern() {
		return argumentGraphPattern;
	}

	public List<Triple> getResultGraphPattern() {
		return resultGraphPattern;
	}

}
