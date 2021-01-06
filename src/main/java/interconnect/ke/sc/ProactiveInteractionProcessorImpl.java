package interconnect.ke.sc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;

public class ProactiveInteractionProcessorImpl implements ProactiveInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ProactiveInteractionProcessorImpl.class);

	private final Set<SingleInteractionProcessor> activeInteractionProcessors;
	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;

	@Override
	public CompletableFuture<AskResult> processAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {

		// in the MVP we interpret the recipient selector as a wildcard.
		// retrieve other knowledge bases
		List<OtherKnowledgeBase> otherKnowledgeBases = otherKnowledgeBaseStore.getOtherKnowledgeBases();
		Set<KnowledgeInteraction> otherKnowledgeInteractions = new HashSet<KnowledgeInteraction>();
		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			otherKnowledgeInteractions.addAll(otherKB.getKnowledgeInteractions());
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new NaiveMatchingProcessor(otherKnowledgeInteractions,
				this.messageReplyTracker);

		activeInteractionProcessors.add(processor);

		CompletableFuture<AskResult> future = new CompletableFuture<AskResult>();
		return future;
	}

//	@Override
//	public CompletableFuture<PostResult> processPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
//			BindingSet someArguments) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
