package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;

/**
 * An object of this class represents that the associated knowledge base will
 * possibly post knowledge of the shape `argument` to the network, and expects
 * a result of the shape `result`.
 */
public final class PostKnowledgeInteraction extends KnowledgeInteraction {

	/**
	 * This graph pattern expresses the 'shape' of knowledge that this
	 * KnowledgeInteraction possibly posts to the network.
	 */
	private final GraphPattern argument;
	
	/**
	 * This graph pattern expresses the 'shape' of knowledge that this
	 * KnowledgeInteraction expects as a result after posting something to the
	 * network.
	 */
	private final GraphPattern result;
	
	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result) {
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
