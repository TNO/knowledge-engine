package interconnect.ke.api;

public final class AnswerKnowledgeInteraction extends KnowledgeInteraction {

	private final GraphPattern pattern;

	public AnswerKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		super(act);
		this.pattern = pattern;
	}

	public GraphPattern getPattern() {
		return pattern;
	}
	
}
