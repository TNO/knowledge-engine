package interconnect.ke.sc;

import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;

public class ProactiveInteractionProcessorImpl implements ProactiveInteractionProcessor {

	@Override
	public CompletableFuture<AskResult> processAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<PostResult> processPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments) {
		// TODO Auto-generated method stub
		return null;
	}

}
