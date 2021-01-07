package interconnect.ke.sc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.lang.arq.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;

public class SerialMatchingProcessor extends SingleInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SerialMatchingProcessor.class);

	private CompletableFuture<AskResult> resultFuture;
	private CompletableFuture<AnswerMessage> messageFuture;
	private Iterator<KnowledgeInteraction> kiIter;
	private AskKnowledgeInteraction myKnowledgeInteraction;
	private BindingSet allBindings;
	private Object lock;

	public SerialMatchingProcessor(Set<KnowledgeInteraction> someKnowledgeInteractions,
			MessageReplyTracker messageReplyTracker) {
		super(someKnowledgeInteractions, messageReplyTracker);
		this.kiIter = someKnowledgeInteractions.iterator();
		this.allBindings = new BindingSet();
		lock = new Object();

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

		synchronized (this.lock) {
			if (kiIter.hasNext()) {
				KnowledgeInteraction ki = kiIter.next();
				AnswerKnowledgeInteraction aKI = null;
				if (ki instanceof AnswerKnowledgeInteraction) {
					aKI = (AnswerKnowledgeInteraction) ki;
					if (matches(myKnowledgeInteraction.getPattern(), aKI.getPattern())) {
						AskMessage askMessage = new AskMessage(null, null, null, null, bindingSet);
						try {
							this.messageFuture = messageReplyTracker.sendAskMessage(askMessage);
							this.messageFuture.thenAccept((aMessage) -> {
								this.messageFuture = null;

								// TODO make sure there are no duplicates
								this.allBindings.addAll(aMessage.getBindings());
								this.checkOtherKnowledgeInteraction(bindingSet);
							});
						} catch (IOException e) {
							LOG.warn("Errors should not occur when sending and processing message: "
									+ askMessage.toString(), e);

							// continue with the work, otherwise this process will come to a halt.
							this.checkOtherKnowledgeInteraction(bindingSet);
						}
					} else {
						this.checkOtherKnowledgeInteraction(bindingSet);
					}
				}
			} else {
				this.resultFuture.complete(new AskResult(this.allBindings));
			}
		}
	}

	private boolean matches(GraphPattern gp1, GraphPattern gp2) {
		return GraphPatternMatcher.checkIsomorph(gp1, gp2);
	}

}
