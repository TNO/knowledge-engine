package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskExchangeInfo;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskResult;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ExchangeInfo;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ExchangeInfo.Initiator;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ExchangeInfo.Status;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostExchangeInfo;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.PostResult;
import eu.interconnectproject.knowledge_engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.KnowledgeInteractionInfo.Type;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;

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
	private final Set<ExchangeInfo> exchangeInfos;
	private Instant previousSend = null;

	public SerialMatchingProcessor(LoggerProvider loggerProvider,
			Set<KnowledgeInteractionInfo> otherKnowledgeInteractions, MessageRouter messageRouter) {
		super(otherKnowledgeInteractions, messageRouter);
		this.LOG = loggerProvider.getLogger(this.getClass());

		this.kiIter = otherKnowledgeInteractions.iterator();
		this.allBindings = new BindingSet();
		this.lock = new Object();
		this.exchangeInfos = new HashSet<>();

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
		this.LOG.trace("processPost()");
		this.myKnowledgeInteraction = postKnowledgeInteraction;
		this.reactFuture = new CompletableFuture<>();
		this.checkOtherKnowledgeInteraction(bindingSet);
		return this.reactFuture;
	}

	private void checkOtherKnowledgeInteraction(BindingSet bindingSet) {

		synchronized (this.lock) {
			if (this.kiIter.hasNext()) {
				KnowledgeInteractionInfo ki = this.kiIter.next();
				if (this.myKnowledgeInteraction.getType() == Type.ASK && ki.getType() == Type.ANSWER) {
					AnswerKnowledgeInteraction aKI = (AnswerKnowledgeInteraction) ki.getKnowledgeInteraction();
					if (this.matches(((AskKnowledgeInteraction) this.myKnowledgeInteraction.getKnowledgeInteraction())
							.getPattern(), aKI.getPattern())) {

						AskMessage askMessage = new AskMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(), bindingSet);
						try {
							this.answerMessageFuture = this.messageRouter.sendAskMessage(askMessage);
							this.previousSend = Instant.now();
							this.answerMessageFuture.thenAccept(aMessage -> {
								try {
									this.answerMessageFuture = null;

									// TODO make sure there are no duplicates
									this.allBindings.addAll(aMessage.getBindings());

									this.exchangeInfos.add(convertMessageToExchangeInfo(aMessage));

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
					} else {
						this.checkOtherKnowledgeInteraction(bindingSet);
					}
				} else if (this.myKnowledgeInteraction.getType() == Type.POST && ki.getType() == Type.REACT) {

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
									this.previousSend = Instant.now();
									this.exchangeInfos.add(convertMessageToExchangeInfo(bindingSet, aMessage));
									// TODO should this statement be moved outside this try/catch, since it cannot
									// throw an exception and it has nothing to do with receiving a message.
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
					this.answerFuture.complete(new AskResult(this.allBindings, this.exchangeInfos));
				} else if (this.reactFuture != null) {
					this.reactFuture.complete(new PostResult(this.allBindings, this.exchangeInfos));
				}
			}
		}
	}

	private AskExchangeInfo convertMessageToExchangeInfo(AnswerMessage aMessage) {

		return new AskExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), aMessage.getBindings(), this.previousSend, Instant.now(),
				Status.SUCCEEDED, null);
	}

	private PostExchangeInfo convertMessageToExchangeInfo(BindingSet argument, ReactMessage aMessage) {

		return new PostExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), argument, aMessage.getBindings(), this.previousSend,
				Instant.now(), Status.SUCCEEDED, null);
	}

	private boolean matches(GraphPattern gp1, GraphPattern gp2) {
		return GraphPatternMatcher.checkIsomorph(gp1, gp2);
	}

}
