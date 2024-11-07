package eu.knowledge.engine.smartconnector.api;

/**
 * An object of this class represents that the associated {@link KnowledgeBase}
 * will possibly post knowledge of the shape `argument` to the network, and
 * expects a result of the shape `result`.
 * 
 * The argument is mandatory, but the result is optional.
 */
public final class PostKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * This {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} possibly posts to the network.
	 */
	private final GraphPattern argument;

	/**
	 * This {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} expects as a result after posting something to
	 * the network.
	 */
	private final GraphPattern result;

	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result) {
		this(act, argument, result, null, false, false, null);
	}

	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result, String name) {
		this(act, argument, result, name, false, false, null);
	}

	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result,
			boolean anIncludeMetaKIs) {
		this(act, argument, result, null, false, anIncludeMetaKIs, null);
	}

	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result, boolean anIsMeta,
			boolean anIncludeMetaKIs) {
		this(act, argument, result, null, anIsMeta, anIncludeMetaKIs, null);
	}

	/**
	 * Create a {@link PostKnowledgeInteraction}. See
	 * {@link KnowledgeInteraction#KnowledgeInteraction(CommunicativeAct, String, boolean, boolean, MatchStrategy)}
	 *
	 * 
	 * @param argument The {@code argument} of this
	 *                 {@link PostKnowledgeInteraction}. It can be seen as the
	 *                 argument of a function call.
	 * @param result   The {@code result} of this {@link PostKnowledgeInteraction}.
	 *                 It can be seen as the result of a function call. Can be
	 *                 {@code null} if this interaction does not expect any result.
	 * @apiNote Can {@code argument} also be {@code null}? No, only the
	 *          {@code result} graph pattern can be {@code null}. Note that not both
	 *          {@code argument} and {@code result} can be {@code null}.
	 */
	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result, String name,
			boolean anIsMeta, boolean anIncludeMetaKIs, MatchStrategy aMatchStrategy) {
		super(act, name, anIsMeta, anIncludeMetaKIs, aMatchStrategy);
		this.argument = argument;
		this.result = result;
	}

	/**
	 * @return This {@link KnowledgeInteraction}'s argument {@link GraphPattern}. It
	 *         can be seen as the argument of a function call.
	 */
	public GraphPattern getArgument() {
		return this.argument;
	}

	/**
	 * @return This {@link KnowledgeInteraction}'s result {@link GraphPattern}. It
	 *         can be seen as the result of a function call. Can be {@code null} if
	 *         this {@link KnowledgeInteraction} does not expect any result.
	 */
	public GraphPattern getResult() {
		return this.result;
	}

	@Override
	public String toString() {
		return "PostKnowledgeInteraction [" + (this.name != null ? "name=" + this.name + ", " : "")
				+ (this.argument != null ? "argument=" + this.argument + ", " : "")
				+ (this.result != null ? "result=" + this.result + ", " : "")
				+ (this.getAct() != null ? "getAct()=" + this.getAct() + ", " : "") + "]";
	}
}
