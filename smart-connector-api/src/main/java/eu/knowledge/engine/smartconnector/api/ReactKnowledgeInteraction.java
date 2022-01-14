package eu.knowledge.engine.smartconnector.api;

import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;

/**
 * An object of this class represents that the associated {@link KnowledgeBase}
 * can react to knowledge of the shape {@code argument}, and can produce a
 * result of the shape {@code result}.
 */
public final class ReactKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * This {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} expects as input.
	 */
	private final GraphPattern argument;

	/**
	 * This {@link GraphPattern} expresses the 'shape' of knowledge that this
	 * {@link KnowledgeInteraction} can produce when receiving input.
	 */
	private final GraphPattern result;

	/**
	 * Creates a {@link ReactKnowledgeInteraction}.
	 *
	 * @param act      The {@link CommunicativeAct} of this
	 *                 {@link KnowledgeInteraction}. It can be read as the 'goal' or
	 *                 'purpose' of the data exchange and whether it has
	 *                 side-effects or not.
	 * @param argument The {@code argument} of this {@link KnowledgeInteraction}. It
	 *                 can be seen as the argument of a function call.
	 * @param result   The {@code result} of this {@link PostKnowledgeInteraction}.
	 *                 It can be seen as the result of a function call. Can be
	 *                 {@code null} if this interaction does not expect any result.
	 * @apiNote TODO Can {@code argument} also be {@code null}? Note that not both
	 *          {@code argument} and {@code result} can be {@code null}.
	 */
	public ReactKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result) {
		this(act, argument, result, false);
	}

	public ReactKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result,
			boolean anIsFullMatch) {
		super(act, false, anIsFullMatch);
		this.argument = argument;
		this.result = result;
	}

	public ReactKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result, boolean anIsMeta,
			boolean anIsFullMatch) {
		super(act, anIsMeta, anIsFullMatch);
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
		return "ReactKnowledgeInteraction [" + (this.argument != null ? "argument=" + this.argument + ", " : "")
				+ (this.result != null ? "result=" + this.result + ", " : "")
				+ (this.getAct() != null ? "getAct()=" + this.getAct() + ", " : "") + "]";
	}

}
