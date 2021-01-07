package interconnect.ke.sc;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public class ReactiveInteractionProcessorImpl implements ReactiveInteractionProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(ReactiveInteractionProcessorImpl.class);
	private MyKnowledgeBaseStore myKnowledgeBaseStore;
	// private MyMetaKnowledgeBase myMetaKnowledgeBase;

	@Override
	public CompletableFuture<AnswerMessage> processAsk(AskMessage anAskMsg) {
		URI answerKnowledgeInteractionId = anAskMsg.getToKnowledgeInteraction();
		AnswerKnowledgeInteraction answerKnowledgeInteraction;
		try {
			answerKnowledgeInteraction = (AnswerKnowledgeInteraction) myKnowledgeBaseStore.getKnowledgeInteractionById(answerKnowledgeInteractionId).getKnowledgeInteraction();
		} catch (Throwable t) {
			LOG.warn("Encountered an unresolvable KnowledgeInteraction ID '" + answerKnowledgeInteractionId + "' that was expected to resolve to one of our own.");
			var future = new CompletableFuture<AnswerMessage>();
			future.completeExceptionally(t);
			return future;
		}
		
		var future = new CompletableFuture<AnswerMessage>();
		{
			BindingSet bindings = null;
			if (answerKnowledgeInteraction.getIsMeta()) {
				// TODO: Ask MyMetaKnowledgeBase for the bindings.
			} else {
				var handler = myKnowledgeBaseStore.getAnswerHandler(answerKnowledgeInteractionId);
				// TODO This should happen in the single thread for the knowledge base
				bindings = handler.answer(answerKnowledgeInteraction, anAskMsg.getBindings());
			}
	
			AnswerMessage result = new AnswerMessage(
				anAskMsg.getToKnowledgeBase(), answerKnowledgeInteractionId,
				anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(),
				anAskMsg.getMessageId(), bindings
			);
			// TODO: Here I just complete the future in the same thread, but we should
			// figure out how to do it asynchronously.
			future.complete(result);
		}
		return future;
	}

	@Override
	public CompletableFuture<ReactMessage> processPost(PostMessage aPostMsg) {
		// TODO Implement after MVP phase.
		return null;
	}
	
}
