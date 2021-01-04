package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;

/**
 * An object of this class represents that the associated knowledge base can
 * provide data that matches the configured GraphPattern `pattern`.
 */
public final class AnswerKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * The graph pattern expresses the 'shape' of knowledge that this
	 * KnowledgeInteraction can provide.
	 */
	private final GraphPattern pattern;

	public AnswerKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		super(act);
		this.pattern = pattern;
	}

	/**
	 * @return This KnowledgeInteraction's graph pattern.
	 */
	public GraphPattern getPattern() {
		return pattern;
	}
	
}
