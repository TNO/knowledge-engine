package eu.interconnectproject.knowledge_engine.reasonerprototype.ki;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;

public abstract class AnswerKnowledgeInteraction extends KnowledgeInteraction {

	private final List<TriplePattern> graphPattern;

	public AnswerKnowledgeInteraction(URI id, TriplePattern... graphPattern) {
		super(id);
		this.graphPattern = Arrays.asList(graphPattern);
	}

	public List<TriplePattern> getGraphPattern() {
		return graphPattern;
	}

}
