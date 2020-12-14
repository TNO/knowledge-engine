package interconnect.ke.api;

import interconnect.ke.api.binding.SolutionSet;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

public interface ReactHandler {

	public SolutionSet react(ReactKnowledgeInteraction aReactKnowledgeInteraction, SolutionSet argument);

}
