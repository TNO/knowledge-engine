package eu.knowledge.engine.smartconnector.api;

import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;

/**
 * An object of this class represents that the associated {@link KnowledgeBase}
 * can provide data that matches the configured {@link GraphPattern}
 * {@code pattern}.
 *
 * In other words, the {@link KnowledgeBase} can answer those kinds of questions
 * for its {@link SmartConnectorImpl}.
 */
public final class AnswerKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * The {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} can provide.
	 */
	private final GraphPattern pattern;

	/**
	 * Create an {@link AnswerKnowledgeInteraction}.
	 *
	 * @param act     The {@link CommunicativeAct} of this
	 *                {@link KnowledgeInteraction}. It can be read as the 'goal' or
	 *                'purpose' of the data exchange and whether it has side-effects
	 *                or not.
	 * @param pattern The {@link GraphPattern} expresses the 'shape' of knowledge
	 *                that this {@link KnowledgeInteraction} can provide.
	 */
	public AnswerKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		this(act, pattern, false);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, boolean anIsFullMatch) {
		super(anAct, false, anIsFullMatch);
		this.pattern = aPattern;
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, boolean anIsMeta,
			boolean anIsFullMatch) {
		super(anAct, anIsMeta, anIsFullMatch);
		this.pattern = aPattern;
	}

	/**
	 * @return This {@link KnowledgeInteraction}'s graph pattern.
	 */
	public GraphPattern getPattern() {
		return this.pattern;
	}

	@Override
	public String toString() {
		return "AnswerKnowledgeInteraction [" + (this.pattern != null ? "pattern=" + this.pattern + ", " : "")
				+ (this.getAct() != null ? "getAct()=" + this.getAct() + ", " : "") + "]";
	}

}
