package eu.knowledge.engine.smartconnector.api;

/**
 * An object of this class represents that the associated {@link KnowledgeBase}
 * can provide data that matches the configured {@link GraphPattern}
 * {@code pattern}.
 *
 * In other words, the {@link KnowledgeBase} can answer those kinds of questions
 * for its {@link SmartConnector}.
 */
public final class AnswerKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * The {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} can provide.
	 */
	private final GraphPattern pattern;

	public AnswerKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		this(act, pattern, null, false, false, null);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, String name) {
		this(act, pattern, name, false, false, null);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, boolean anIncludeMetaKIs) {
		this(anAct, aPattern, null, false, anIncludeMetaKIs, null);
	}

	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, boolean anIsMeta,
			boolean anIncludeMetaKIs) {
		this(anAct, aPattern, null, anIsMeta, anIncludeMetaKIs, null);
	}

	/**
	 * Create an {@link AnswerKnowledgeInteraction}. See
	 * {@link KnowledgeInteraction#KnowledgeInteraction(CommunicativeAct, String, boolean, boolean, boolean, MatchStrategy)}
	 *
	 * @param aPattern The {@link GraphPattern} expresses the 'shape' of knowledge
	 *                 that this {@link KnowledgeInteraction} can provide.
	 */
	public AnswerKnowledgeInteraction(CommunicativeAct anAct, GraphPattern aPattern, String aName, boolean anIsMeta,
			boolean anIncludeMetaKIs, MatchStrategy aMatchStrategy) {
		super(anAct, aName, anIsMeta, anIncludeMetaKIs, false, aMatchStrategy);
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
