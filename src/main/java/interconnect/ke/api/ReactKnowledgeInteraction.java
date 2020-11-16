package interconnect.ke.api;

public final class ReactKnowledgeInteraction extends KnowledgeInteraction {

	private final GraphPattern argument;
	private final GraphPattern result;
	
	public ReactKnowledgeInteraction(CommunicativeAct act, GraphPattern argument, GraphPattern result) {
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
