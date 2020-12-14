package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeInteraction;

public final class PostKnowledgeInteraction extends KnowledgeInteraction {

	private final GraphPattern argument;
	private final GraphPattern result;
	
	public PostKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result) {
		super(act);
		this.argument = argument;
		this.result = result;
	}

	public GraphPattern getArgument() {
		return argument;
	}

	public GraphPattern getResult() {
		return result;
	}
}
