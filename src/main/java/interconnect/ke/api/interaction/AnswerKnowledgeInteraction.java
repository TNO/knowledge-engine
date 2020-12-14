package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeInteraction;

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
