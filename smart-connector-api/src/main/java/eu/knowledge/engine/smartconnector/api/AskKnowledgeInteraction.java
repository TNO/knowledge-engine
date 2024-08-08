package eu.knowledge.engine.smartconnector.api;

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
		this(act, pattern, null, false, false);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, String name) {
		this(act, pattern, name, false, false);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, boolean anIncludeMetaKIs) {
		this(act, pattern, null, false, anIncludeMetaKIs);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, boolean anisMeta,
			boolean anIncludeMetaKIs) {
		this(act, pattern, null, anisMeta, anIncludeMetaKIs);
	}

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern, String name, boolean anisMeta,
			boolean anIncludeMetaKIs) {
		super(act, name, anisMeta, anIncludeMetaKIs);
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
