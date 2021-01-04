package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;

/**
 * An object of this class represents that the associated knowledge base will
 * possibly ask for data that matches the configured GraphPattern `pattern`.
 */
public final class AskKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * The graph pattern expresses the 'shape' of knowledge that this knowledge
	 * interaction possibly asks for.
	 */
	private final GraphPattern pattern;

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		super(act);
		this.pattern = pattern;
	}

	/**
	 * @return This knowledge interaction's graph pattern.
	 */
	public GraphPattern getPattern() {
		return pattern;
	}
}
