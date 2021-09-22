package eu.interconnectproject.knowledge_engine.reasonerprototype.ki;

import java.net.URI;
import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;

public abstract class ReactKnowledgeInteraction extends KnowledgeInteraction {

	private final List<TriplePattern> argumentGraphPattern;
	private final List<TriplePattern> resultGraphPattern;

	public ReactKnowledgeInteraction(URI id, List<TriplePattern> argumentGraphPattern, List<TriplePattern> resultGraphPattern) {
		super(id);
		this.argumentGraphPattern = argumentGraphPattern;
		this.resultGraphPattern = resultGraphPattern;
	}

	public List<TriplePattern> getArgumentGraphPattern() {
		return argumentGraphPattern;
	}

	public List<TriplePattern> getResultGraphPattern() {
		return resultGraphPattern;
	}

}
