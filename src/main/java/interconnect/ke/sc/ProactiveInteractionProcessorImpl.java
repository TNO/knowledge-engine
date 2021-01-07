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
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.ReactMessage;

public class ProactiveInteractionProcessorImpl implements ProactiveInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ProactiveInteractionProcessorImpl.class);

	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private final MessageReplyTracker messageReplyTracker;

	private MessageDispatcherEndpoint messageDispatcherEndpoint;

	public ProactiveInteractionProcessorImpl(OtherKnowledgeBaseStore otherKnowledgeBaseStore) {
		super();
		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
		this.setMessageDispatcherEndpoint(messageDispatcherEndpoint);
		this.messageReplyTracker = new MessageReplyTracker(this.messageDispatcherEndpoint);
	}

	@Override
	public CompletableFuture<AskResult> processAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {

		assert anAKI != null : "the knowledge interaction should be non-null";
		assert aBindingSet != null : "the binding set should be non-null";

		// TODO use RecipientSelector. In the MVP we interpret the recipient selector as
		// a wildcard.

		// retrieve other knowledge bases
		List<OtherKnowledgeBase> otherKnowledgeBases = otherKnowledgeBaseStore.getOtherKnowledgeBases();
		Set<KnowledgeInteraction> otherKnowledgeInteractions = new HashSet<KnowledgeInteraction>();

		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			otherKnowledgeInteractions.addAll(otherKB.getKnowledgeInteractions());
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new SerialMatchingProcessor(otherKnowledgeInteractions,
				this.messageReplyTracker);

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.
		CompletableFuture<AskResult> future = processor.processInteraction(anAKI, aBindingSet);

		return future;
	}

	@Override
	public void setMessageDispatcherEndpoint(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		this.messageDispatcherEndpoint = messageDispatcherEndpoint;
	}

	@Override
	public void unsetMessageDispatcherEndpoint() {
		this.messageDispatcherEndpoint = null;
	}

	@Override
	public void handleAnswerMessage(AnswerMessage answerMessage) {
		this.messageReplyTracker.handleAnswerMessage(answerMessage);

	}

	@Override
	public void handleReactMessage(ReactMessage reactMessage) {
		this.messageReplyTracker.handleReactMessage(reactMessage);
	}

}
