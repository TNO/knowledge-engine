package interconnect.ke.sc;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;

public class SerialMatchingProcessor extends SingleInteractionProcessor {

	private CompletableFuture<AskResult> resultFuture;
	private CompletableFuture<AnswerMessage> messageFuture;
	private Iterator<KnowledgeInteraction> kiIter;
	private AskKnowledgeInteraction myKnowledgeInteraction;
	private BindingSet allBindings;

	public SerialMatchingProcessor(Set<KnowledgeInteraction> someKnowledgeInteractions,
			MessageReplyTracker messageReplyTracker) {
		super(someKnowledgeInteractions, messageReplyTracker);
		this.kiIter = someKnowledgeInteractions.iterator();
		this.allBindings = new BindingSet();
	}

	@Override
	CompletableFuture<AskResult> processInteraction(AskKnowledgeInteraction askKnowledgeInteraction,
			BindingSet bindingSet) {
		myKnowledgeInteraction = askKnowledgeInteraction;
		resultFuture = new CompletableFuture<AskResult>();
		checkOtherKnowledgeInteraction(bindingSet);
		return resultFuture;
	}

	private void checkOtherKnowledgeInteraction(BindingSet bindingSet) {

		if (kiIter.hasNext()) {
			KnowledgeInteraction ki = kiIter.next();
			AnswerKnowledgeInteraction aKI = null;
			if (ki instanceof AnswerKnowledgeInteraction) {
				aKI = (AnswerKnowledgeInteraction) ki;
				if (matches(myKnowledgeInteraction.getPattern(), aKI.getPattern())) {
					AskMessage askMessage = new AskMessage(null, null, null, null, bindingSet);
					this.messageFuture = messageReplyTracker.sendAskMessage(askMessage);
					this.messageFuture.thenAccept((aMessage) -> {
						this.messageFuture = null;

						// TODO make sure there are no duplicates
						this.allBindings.addAll(aMessage.getBindings());
						this.checkOtherKnowledgeInteraction(bindingSet);
					});
				} else {
					this.checkOtherKnowledgeInteraction(bindingSet);
				}
			}
		} else {
			this.resultFuture.complete(new AskResult(this.allBindings));
		}
	}

	private boolean matches(GraphPattern gp1, GraphPattern gp2) {
		return true;
	}

}
