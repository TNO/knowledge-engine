package interconnect.ke.sc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.sc.KnowledgeInteractionInfo.Type;

public class SerialMatchingProcessor extends SingleInteractionProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(SerialMatchingProcessor.class);

	private CompletableFuture<AskResult> answerFuture;
	private CompletableFuture<PostResult> reactFuture;
	private CompletableFuture<AnswerMessage> answerMessageFuture;
	private CompletableFuture<ReactMessage> reactMessageFuture;
	private final Iterator<KnowledgeInteractionInfo> kiIter;
	private MyKnowledgeInteractionInfo myKnowledgeInteraction;
	private final BindingSet allBindings;
	private final Object lock;

	public SerialMatchingProcessor(Set<KnowledgeInteractionInfo> otherKnowledgeInteractions,
			MessageRouter messageRouter) {
		super(otherKnowledgeInteractions, messageRouter);
		this.kiIter = otherKnowledgeInteractions.iterator();
		this.allBindings = new BindingSet();
		this.lock = new Object();

	}

	@Override
	CompletableFuture<AskResult> processAskInteraction(MyKnowledgeInteractionInfo askKnowledgeInteraction,
			BindingSet bindingSet) {
		this.myKnowledgeInteraction = askKnowledgeInteraction;
		this.answerFuture = new CompletableFuture<>();
		this.checkOtherKnowledgeInteraction(bindingSet);
		return this.answerFuture;
	}

	@Override
	CompletableFuture<PostResult> processPostInteraction(MyKnowledgeInteractionInfo postKnowledgeInteraction,
			BindingSet bindingSet) {
		this.myKnowledgeInteraction = postKnowledgeInteraction;
		this.reactFuture = new CompletableFuture<>();
		this.checkOtherKnowledgeInteraction(bindingSet);
		return this.reactFuture;
	}

	private void checkOtherKnowledgeInteraction(BindingSet bindingSet) {

		synchronized (this.lock) {
			if (this.kiIter.hasNext()) {
				KnowledgeInteractionInfo ki = this.kiIter.next();
				if (ki.getType() == Type.ANSWER) {
					AnswerKnowledgeInteraction aKI = (AnswerKnowledgeInteraction) ki.getKnowledgeInteraction();
					if (this.matches(((AskKnowledgeInteraction) this.myKnowledgeInteraction.getKnowledgeInteraction())
							.getPattern(), aKI.getPattern())) {

						AskMessage askMessage = new AskMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(), bindingSet);
						try {
							this.answerMessageFuture = this.messageRouter.sendAskMessage(askMessage);
							this.answerMessageFuture.thenAccept((aMessage) -> {
								this.answerMessageFuture = null;

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
					}
				} else if (ki.getType() == Type.REACT) {
					ReactKnowledgeInteraction rKI = (ReactKnowledgeInteraction) ki.getKnowledgeInteraction();
					if (this.matches(((PostKnowledgeInteraction) this.myKnowledgeInteraction.getKnowledgeInteraction()).getArgument(), rKI.getArgument())) {
						PostMessage postMessage = new PostMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(), bindingSet);
						try {
							this.reactMessageFuture = this.messageRouter.sendPostMessage(postMessage);
							this.reactMessageFuture.thenAccept((aMessage) -> {
								this.reactMessageFuture = null;

								// TODO make sure there are no duplicates
								this.allBindings.addAll(aMessage.getBindings());
								this.checkOtherKnowledgeInteraction(bindingSet);
							});
						} catch (IOException e) {
							LOG.warn("Errors should not occur when sending and processing message: "
									+ postMessage.toString(), e);

							// continue with the work, otherwise this process will come to a halt.
							this.checkOtherKnowledgeInteraction(bindingSet);
						}
					} else {
						this.checkOtherKnowledgeInteraction(bindingSet);
					}
				}
			} else {
				if (this.answerFuture != null) {
					this.answerFuture.complete(new AskResult(this.allBindings));
				} else if (this.reactFuture != null) {
					this.reactFuture.complete(new PostResult(this.allBindings));
				}
			}
		}
	}

	private boolean matches(GraphPattern gp1, GraphPattern gp2) {
		return GraphPatternMatcher.checkIsomorph(gp1, gp2);
	}

}
