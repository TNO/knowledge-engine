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
	// private MyKnowledgeBaseStore myKnowledgeBaseStore;
	// private MyMetaKnowledgeBase myMetaKnowledgeBase;

	@Override
	public CompletableFuture<AnswerMessage> processAsk(AskMessage anAskMsg) {
		// look at the graph pattern of anAskMsg.toKnowledgeInteraction
		URI oneofOurKIs = anAskMsg.getToKnowledgeInteraction(); // This should be asn URI to one of our KIs
		KnowledgeInteraction my;
		try {
			my = null; // TODO: Ask the MyKnowledgeBaseStore to dereference the KI.
		} catch (Throwable t) {
			LOG.warn("Encountered an unresolvable KnowledgeInteraction ID '" + oneofOurKIs + "' that was expected to resolve to one of our own.");
			var future = new CompletableFuture<AnswerMessage>();
			future.completeExceptionally(t);
			return future;
		}
		
		var future = new CompletableFuture<AnswerMessage>();
		BindingSet bindings = new BindingSet();
		if (my.getIsMeta()) {
			// TODO: Ask MyMetaKnowledgeBase for the bindings.
		} else {
			if (my instanceof AnswerKnowledgeInteraction) {
				// TODO: Ask the MyKnowledgeBaseStore for the handler and call it to get the bindings (in a new thread?)
			} else {
				LOG.warn("Encountered a KnowledgeInteraction with ID '" + oneofOurKIs + "' that unexpectedly wasn't an AnswerKnowledgeInteraction.");
				future.completeExceptionally(new Exception("This AskMessage was unable to be ansered because it looks like it doesn't have an AnswerKnowledgeInteraction."));
				return future;
			}
		}
		AnswerMessage result = new AnswerMessage(anAskMsg.getToKnowledgeBase(), oneofOurKIs, anAskMsg.getFromKnowledgeBase(), anAskMsg.getFromKnowledgeInteraction(), anAskMsg.getMessageId(), bindings);
		future.complete(result);
		return future;
	}

	@Override
	public CompletableFuture<ReactMessage> processPost(PostMessage aPostMsg) {
		// TODO Implement after MVP phase.
		return null;
	}
	
}
