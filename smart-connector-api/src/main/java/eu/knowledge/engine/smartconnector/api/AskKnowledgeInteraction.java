package eu.knowledge.engine.smartconnector.api;

import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;

/**
 * An object of this class represents that the associated {@link KnowledgeBase}
 * will possibly ask for data that matches the configured {@link GraphPattern}
 * {@code pattern}.
 *
 * In other words, the {@link KnowledgeBase} asks these kinds of questions to
 * its {@link SmartConnectorImpl}.
 */
public final class AskKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * The {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} possibly asks for.
	 */
	private final GraphPattern pattern;

	/**
	 * Create a {@link AskKnowledgeInteraction}.
	 *
	 * @param act     The {@link CommunicativeAct} of this
	 *                {@link KnowledgeInteraction}. It can be read as the 'goal' or
	 *                'purpose' of the data exchange and whether it has side-effects
	 *                or not.
	 * @param pattern The {@link GraphPattern} expresses the 'shape' of knowledge
	 *                that this {@link KnowledgeInteraction} asks for.
	 */
	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		super(act);
		this.pattern = pattern;
	}

	/**
	 * @return This {@link KnowledgeInteraction}'s graph pattern.
	 */
	public GraphPattern getPattern() {
		return this.pattern;
	}

	@Override
	public String toString() {
		return "AskKnowledgeInteraction [" + (this.pattern != null ? "pattern=" + this.pattern + ", " : "")
				+ (this.getAct() != null ? "getAct()=" + this.getAct() + ", " : "") + "]";
	}
}
