package interconnect.ke.api;

import interconnect.ke.api.binding.SolutionSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;

public interface AnswerHandler {

	public SolutionSet answer(AnswerKnowledgeInteraction kAKI, SolutionSet aSolution);
}
