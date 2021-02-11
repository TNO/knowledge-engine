package interconnect.ke.sc;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public class InteractionProcessorImpl implements InteractionProcessor {

	private final Logger LOG;

	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private MessageRouter messageRouter;
	private final KnowledgeBaseStore myKnowledgeBaseStore;
	private final MetaKnowledgeBase metaKnowledgeBase;

	private final LoggerProvider loggerProvider;

	public InteractionProcessorImpl(LoggerProvider loggerProvider, OtherKnowledgeBaseStore otherKnowledgeBaseStore,
			KnowledgeBaseStore myKnowledgeBaseStore, MetaKnowledgeBase metaKnowledgeBase) {
		super();
		this.loggerProvider = loggerProvider;
		this.LOG = loggerProvider.getLogger(this.getClass());
		this.otherKnowledgeBaseStore = otherKnowledgeBaseStore;
		this.myKnowledgeBaseStore = myKnowledgeBaseStore;
		this.metaKnowledgeBase = metaKnowledgeBase;
	}

	@Override
	public CompletableFuture<AskResult> processAskFromKnowledgeBase(MyKnowledgeInteractionInfo anAKI,
			RecipientSelector aSelector, BindingSet aBindingSet) {
		assert anAKI != null : "the knowledge interaction should be non-null";
		assert aBindingSet != null : "the binding set should be non-null";

		var myKnowledgeInteraction = anAKI.getKnowledgeInteraction();

		// TODO use RecipientSelector. In the MVP we interpret the recipient selector as
		// a wildcard.

		// retrieve other knowledge bases
		Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();
		Set<KnowledgeInteractionInfo> otherKnowledgeInteractions = new HashSet<>();

		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			// Use the knowledge interactions from the other KB
			var knowledgeInteractions = otherKB.getKnowledgeInteractions().stream().filter((r) -> {
				// But filter on the communicative act. These have to match!
				return myKnowledgeInteraction.getAct().matches(r.getKnowledgeInteraction().getAct());
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new SerialMatchingProcessor(this.loggerProvider,
				otherKnowledgeInteractions, this.messageRouter);

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.
		CompletableFuture<AskResult> future = processor.processAskInteraction(anAKI, aBindingSet);

		return future;
	}

	@Override
	public CompletableFuture<AnswerMessage> processAskFromMessageRouter(AskMessage anAskMsg) {
		URI answerKnowledgeInteractionId = anAskMsg.getToKnowledgeInteraction();

		try {
			KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
					.getKnowledgeInteractionById(answerKnowledgeInteractionId);

			AnswerKnowledgeInteraction answerKnowledgeInteraction;
			answerKnowledgeInteraction = (AnswerKnowledgeInteraction) knowledgeInteractionById
					.getKnowledgeInteraction();
			var future = new CompletableFuture<AnswerMessage>();
			{
				BindingSet bindings = null;
				if (knowledgeInteractionById.isMeta()) {
					// TODO: Ask MyMetaKnowledgeBase for the bindings.
				} else {
					var handler = this.myKnowledgeBaseStore.getAnswerHandler(answerKnowledgeInteractionId);
					// TODO This should happen in the single thread for the knowledge base
					bindings = handler.answer(answerKnowledgeInteraction, anAskMsg.getBindings());
				}

				AnswerMessage result = new AnswerMessage(anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
						anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(),
						anAskMsg.getMessageId(), bindings);
				// TODO: Here I just complete the future in the same thread, but we should
				// figure out how to do it asynchronously.
				future.complete(result);

				return future;
			}
		} catch (Throwable t) {
			this.LOG.warn("Encountered an unresolvable KnowledgeInteraction ID '" + answerKnowledgeInteractionId
					+ "' that was expected to resolve to one of our own.", t);
			var future = new CompletableFuture<AnswerMessage>();
			future.completeExceptionally(t);
			return future;
		}
	}

	@Override
	public CompletableFuture<PostResult> processPostFromKnowledgeBase(MyKnowledgeInteractionInfo aPKI,
			RecipientSelector aSelector, BindingSet someArguments) {
		assert aPKI != null : "the knowledge interaction should be non-null";
		assert someArguments != null : "the binding set should be non-null";

		var myKnowledgeInteraction = aPKI.getKnowledgeInteraction();

		// TODO: Use RecipientSelector

		// retrieve other knowledge bases
		Set<OtherKnowledgeBase> otherKnowledgeBases = this.otherKnowledgeBaseStore.getOtherKnowledgeBases();
		Set<KnowledgeInteractionInfo> otherKnowledgeInteractions = new HashSet<>();

		for (OtherKnowledgeBase otherKB : otherKnowledgeBases) {
			// Use the knowledge interactions from the other KB
			var knowledgeInteractions = otherKB.getKnowledgeInteractions().stream().filter((r) -> {
				// But filter on the communicative act. These have to match!
				return myKnowledgeInteraction.getAct().matches(r.getKnowledgeInteraction().getAct());
			});
			otherKnowledgeInteractions.addAll(knowledgeInteractions.collect(Collectors.toList()));
		}

		// create a new SingleInteractionProcessor to handle this ask.
		SingleInteractionProcessor processor = new SerialMatchingProcessor(this.loggerProvider,
				otherKnowledgeInteractions, this.messageRouter);

		// give the caller something to chew on while it waits. This method starts the
		// interaction process as far as it can until it is blocked because it waits for
		// outstanding message replies. Then it returns the future. Threads from the
		// MessageDispatcher will finish this process and the thread that handles the
		// last reply message will complete the future and notify the caller
		// KnowledgeBase.
		CompletableFuture<PostResult> future = processor.processPostInteraction(aPKI, someArguments);

		return future;
	}

	@Override
	public CompletableFuture<ReactMessage> processPostFromMessageRouter(PostMessage aPostMsg) {
		URI reactKnowledgeInteractionId = aPostMsg.getToKnowledgeInteraction();
		try {
			KnowledgeInteractionInfo knowledgeInteractionById = this.myKnowledgeBaseStore
					.getKnowledgeInteractionById(reactKnowledgeInteractionId);
			ReactKnowledgeInteraction reactKnowledgeInteraction;
			reactKnowledgeInteraction = (ReactKnowledgeInteraction) knowledgeInteractionById.getKnowledgeInteraction();
			var future = new CompletableFuture<ReactMessage>();
			{
				BindingSet bindings = null;
				if (knowledgeInteractionById.isMeta()) {
					// TODO: Ask MyMetaKnowledgeBase for the bindings.
				} else {
					var handler = this.myKnowledgeBaseStore.getReactHandler(reactKnowledgeInteractionId);
					// TODO This should happen in the single thread for the knowledge base
					bindings = handler.react(reactKnowledgeInteraction, aPostMsg.getBindings());
				}

				ReactMessage result = new ReactMessage(aPostMsg.getToKnowledgeBase(), reactKnowledgeInteractionId,
						aPostMsg.getFromKnowledgeBase(), aPostMsg.getFromKnowledgeInteraction(),
						aPostMsg.getMessageId(), bindings);
				// TODO: Here I just complete the future in the same thread, but we should
				// figure out how to do it asynchronously.
				future.complete(result);
			}

			return future;
		} catch (Throwable t) {
			this.LOG.warn("Encountered an unresolvable KnowledgeInteraction ID '" + reactKnowledgeInteractionId
					+ "' that was expected to resolve to one of our own.", t);
			var future = new CompletableFuture<ReactMessage>();
			future.completeExceptionally(t);
			return future;
		}

	}

	@Override
	public void setMessageRouter(MessageRouter messageRouter) {
		this.messageRouter = messageRouter;
	}

	@Override
	public void unsetMessageRouter() {
		this.messageRouter = null;
	}

}
