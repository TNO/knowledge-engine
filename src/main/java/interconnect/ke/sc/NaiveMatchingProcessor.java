package interconnect.ke.sc;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;

public class NaiveMatchingProcessor extends SingleInteractionProcessor {

	public NaiveMatchingProcessor(Set<KnowledgeInteraction> someKnowledgeInteractions,
			ProactiveInteractionProcessor messageDispatcher) {
		super(someKnowledgeInteractions, messageDispatcher);

		
		
	}

	@Override
	CompletableFuture<BindingSet> processInteraction(AskKnowledgeInteraction aAKI, BindingSet someBindings) {
		
		return null;
	}

}
