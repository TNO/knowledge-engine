package interconnect.ke.sc;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;

public class ProactiveInteractionProcessorImpl implements ProactiveInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ProactiveInteractionProcessorImpl.class);

	private final Map<SingleInteractionProcessor, CompletableFuture<AskResult>> processorToFutureMapping;
	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private final MessageReplyTracker messageReplyTracker;

	public ProactiveInteractionProcessorImpl(
			Map<SingleInteractionProcessor, CompletableFuture<AskResult>> processorToFutureMapping,
			OtherKnowledgeBaseStore otherKnowledgeBaseStore, MessageReplyTracker messageReplyTracker) {
		super();
		this.processorToFutureMapping = processorToFutureMapping;
		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
		this.messageReplyTracker = messageReplyTracker;
	}

	@Override
	public CompletableFuture<AskResult> processAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {

		assert anAKI != null : "the knowledge interaction should be non-null";
		assert aBindingSet != null : "the binding set should be non-null";

		// in the MVP we interpret the recipient selector as a wildcard.

		// retrieve other knowledge bases
		List<OtherKnowledgeBase> otherKnowledgeBases = otherKnowledgeBaseStore.getOtherKnowledgeBases();

		assert otherKnowledgeBases != null;

		Set<KnowledgeInteraction> otherKnowledgeInteractions = new HashSet<KnowledgeInteraction>();

		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			otherKnowledgeInteractions.addAll(otherKB.getKnowledgeInteractions());
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new SerialMatchingProcessor(otherKnowledgeInteractions,
				this.messageReplyTracker);

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. THe
		// MessageDispatcher threads will finish the process and the last reply message
		// will complete the
		// future.
		CompletableFuture<AskResult> future = processor.processInteraction(anAKI, aBindingSet);

		// store the interactionprocessor for future usage.
		processorToFutureMapping.put(processor, future);

		return future;
	}

//	@Override
//	public CompletableFuture<PostResult> processPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
//			BindingSet someArguments) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
