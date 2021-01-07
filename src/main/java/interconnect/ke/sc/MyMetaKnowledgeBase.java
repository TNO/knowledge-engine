package interconnect.ke.sc;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;

public interface MyMetaKnowledgeBase {
	BindingSet answer(AnswerKnowledgeInteraction anAKI, BindingSet incomingBindings);
}
