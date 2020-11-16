package interconnect.ke.api;

public final class AskKnowledgeInteraction extends KnowledgeInteraction {

	private final GraphPattern pattern;

	public AskKnowledgeInteraction(CommunicativeAct act, GraphPattern pattern) {
		super(act);
		this.pattern = pattern;
	}

	public GraphPattern getPattern() {
		return pattern;
	}
}
