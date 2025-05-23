package eu.knowledge.engine.smartconnector.api;

/**
 * An object of this class represents that the associated {@link KnowledgeBase}
 * will possibly ask for data that matches the configured {@link GraphPattern}
 * {@code pattern}.
 *
 * In other words, the {@link KnowledgeBase} asks these kinds of questions to
 * its {@link SmartConnector}.
 */
public final class AskKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * The {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} possibly asks for.
	 */
	private final GraphPattern pattern;

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		this(act, pattern, null, false, false, false, null);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, String name,
			boolean aKnowledgeGapsEnabled) {
		this(act, pattern, name, false, false, aKnowledgeGapsEnabled, null);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, boolean anIncludeMetaKIs) {
		this(act, pattern, null, false, anIncludeMetaKIs, false, null);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, boolean anisMeta,
			boolean anIncludeMetaKIs) {
		this(act, pattern, null, anisMeta, anIncludeMetaKIs, false, null);
	}

	/**
	 * Create a {@link AskKnowledgeInteraction}. See
	 * {@link KnowledgeInteraction#KnowledgeInteraction(CommunicativeAct, String, boolean, boolean, boolean, MatchStrategy)}.
	 *
	 * @param pattern The {@link GraphPattern} expresses the 'shape' of knowledge
	 *                that this {@link KnowledgeInteraction} asks for.
	 */
	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, String name, boolean anisMeta,
			boolean anIncludeMetaKIs, boolean aKnowledgeGapsEnabled, MatchStrategy aMatchStrategy) {
		super(act, name, anisMeta, anIncludeMetaKIs, aKnowledgeGapsEnabled, aMatchStrategy);
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
		return "AskKnowledgeInteraction [" + (this.name != null ? "name=" + this.name + ", " : "")
				+ (this.pattern != null ? "pattern=" + this.pattern + ", " : "")
				+ (this.getAct() != null ? "getAct()=" + this.getAct() + ", " : "") + "]";
	}
}
