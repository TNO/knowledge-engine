package eu.interconnectproject.knowledge_engine.reasonerprototype.ki;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public abstract class AnswerKnowledgeInteraction extends KnowledgeInteraction {

	private final List<Triple> graphPattern;

	public AnswerKnowledgeInteraction(URI id, Triple... graphPattern) {
		super(id);
		this.graphPattern = Arrays.asList(graphPattern);
	}

	public List<Triple> getGraphPattern() {
		return graphPattern;
	}

}
