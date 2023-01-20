package eu.knowledge.engine.smartconnector.api;

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
		this(act, pattern, null, false, false);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, String name) {
		this(act, pattern, name, false, false);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, boolean anIsFullMatch) {
		this(anAct, aPattern, null, false, anIsFullMatch);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, boolean anIsMeta,
			boolean anIsFullMatch) {
		this(anAct, aPattern, null, anIsMeta, anIsFullMatch);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, String name, boolean anIsMeta,
			boolean anIsFullMatch) {
		super(anAct, name, anIsMeta, anIsFullMatch);
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
		return "AnswerKnowledgeInteraction [" + (this.name != null ? "name=" + this.name + ", " : "")
				+ (this.pattern != null ? "pattern=" + this.pattern + ", " : "")
				+ (this.getAct() != null ? "getAct()=" + this.getAct() + ", " : "") + "]";
	}

}
