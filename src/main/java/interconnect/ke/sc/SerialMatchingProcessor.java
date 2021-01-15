package interconnect.ke.sc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.sc.KnowledgeInteractionInfo.Type;

public class SerialMatchingProcessor extends SingleInteractionProcessor {

	private final Logger LOG;

	private CompletableFuture<AskResult> answerFuture;
	private CompletableFuture<PostResult> reactFuture;
	private CompletableFuture<AnswerMessage> answerMessageFuture;
	private CompletableFuture<ReactMessage> reactMessageFuture;
	private final Iterator<KnowledgeInteractionInfo> kiIter;
	private MyKnowledgeInteractionInfo myKnowledgeInteraction;
	private final BindingSet allBindings;
	private final Object lock;

	public SerialMatchingProcessor(LoggerProvider loggerProvider,
			Set<KnowledgeInteractionInfo> otherKnowledgeInteractions, MessageRouter messageRouter) {
		super(otherKnowledgeInteractions, messageRouter);
		this.LOG = loggerProvider.getLogger(this.getClass());

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

		this.LOG.trace("Before sync: {}", bindingSet);
		synchronized (this.lock) {
			this.LOG.trace("In sync: hasNext? {}", this.kiIter.hasNext());
			if (this.kiIter.hasNext()) {
				KnowledgeInteractionInfo ki = this.kiIter.next();
				this.LOG.trace("In sync: {}", ki);
				if (ki.getType() == Type.ANSWER) {
					AnswerKnowledgeInteraction aKI = (AnswerKnowledgeInteraction) ki.getKnowledgeInteraction();
					if (this.matches(((AskKnowledgeInteraction) this.myKnowledgeInteraction.getKnowledgeInteraction())
							.getPattern(), aKI.getPattern())) {
						this.LOG.trace("In sync: after match");
						AskMessage askMessage = new AskMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(), bindingSet);
						try {
							this.LOG.trace("Before sending message {}", askMessage);
							this.answerMessageFuture = this.messageRouter.sendAskMessage(askMessage);
							this.LOG.trace("After sending message {}. Expecting answer.", askMessage);
							this.answerMessageFuture.thenAccept(aMessage -> {
								try {
									this.LOG.trace("Received message {}. Is answer to: {}", aMessage, askMessage);
									this.answerMessageFuture = null;

									// TODO make sure there are no duplicates
									this.allBindings.addAll(aMessage.getBindings());
									this.checkOtherKnowledgeInteraction(bindingSet);
								} catch (Throwable t) {
									this.LOG.error("Receiving a answer message should succeed.", t);
								}
							});
						} catch (IOException e) {
							this.LOG.warn("Errors should not occur when sending and processing message: "
									+ askMessage.toString(), e);

							// continue with the work, otherwise this process will come to a halt.
							this.checkOtherKnowledgeInteraction(bindingSet);
						}
					}
				} else if (ki.getType() == Type.REACT) {
					ReactKnowledgeInteraction rKI = (ReactKnowledgeInteraction) ki.getKnowledgeInteraction();
					if (this.matches(((PostKnowledgeInteraction) this.myKnowledgeInteraction.getKnowledgeInteraction())
							.getArgument(), rKI.getArgument())) {
						PostMessage postMessage = new PostMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(), bindingSet);
						try {
							this.reactMessageFuture = this.messageRouter.sendPostMessage(postMessage);
							this.reactMessageFuture.thenAccept(aMessage -> {
								try {
									this.reactMessageFuture = null;

									// TODO make sure there are no duplicates
									this.allBindings.addAll(aMessage.getBindings());
									this.checkOtherKnowledgeInteraction(bindingSet);
								} catch (Throwable t) {
									this.LOG.error("Receiving a react message should succeed.", t);
								}
							});
						} catch (IOException e) {
							this.LOG.warn("Errors should not occur when sending and processing message: "
									+ postMessage.toString(), e);

							// continue with the work, otherwise this process will come to a halt.
							this.checkOtherKnowledgeInteraction(bindingSet);
						}
					} else {
						this.checkOtherKnowledgeInteraction(bindingSet);
					}
				} else {
					this.checkOtherKnowledgeInteraction(bindingSet);
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
