package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;

/**
 * An object of this class represents that the associated knowledge base can
 * react to knowledge of the shape `argument`, and can produce a result of the
 * shape `result`.
 */
public final class ReactKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * This graph pattern expresses the 'shape' of knowledge that this
	 * KnowledgeInteraction expects as input.
	 */
	private final GraphPattern argument;

	/**
	 * This graph pattern expresses the 'shape' of knowledge that this
	 * KnowledgeInteraction can produce when receiving input.
	 */
	private final GraphPattern result;
	
	public ReactKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result) {
		super(act);
		this.argument = argument;
		this.result = result;
	}

	/**
	 * @return This KnowledgeInteraction's argument graph pattern.
	 */
	public GraphPattern getArgument() {
		return argument;
	}

	/**
	 * @return This KnowledgeInteraction's result graph pattern.
	 */
	public GraphPattern getResult() {
		return result;
	}

}
