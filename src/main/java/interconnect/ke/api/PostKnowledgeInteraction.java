package interconnect.ke.api;

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
