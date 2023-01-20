package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.lang.arq.ParseException;
import org.slf4j.Logger;

import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskExchangeInfo;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.PostExchangeInfo;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Initiator;
import eu.knowledge.engine.smartconnector.api.ExchangeInfo.Status;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo.Type;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;

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
	private final Set<AskExchangeInfo> askExchangeInfos;
	private final Set<PostExchangeInfo> postExchangeInfos;
	private Instant previousSend = null;

	public SerialMatchingProcessor(LoggerProvider loggerProvider,
			Set<KnowledgeInteractionInfo> otherKnowledgeInteractions, MessageRouter messageRouter) {
		super(otherKnowledgeInteractions, messageRouter);
		this.LOG = loggerProvider.getLogger(this.getClass());

		this.kiIter = otherKnowledgeInteractions.iterator();
		this.allBindings = new BindingSet();
		this.lock = new Object();
		this.askExchangeInfos = new HashSet<>();
		this.postExchangeInfos = new HashSet<>();

	}

	@Override
	void planAskInteraction(MyKnowledgeInteractionInfo aAKI) {
		this.myKnowledgeInteraction = aAKI;
	}

	@Override
	CompletableFuture<AskResult> executeAskInteraction(BindingSet bindingSet) {
		this.answerFuture = new CompletableFuture<>();
		this.checkOtherKnowledgeInteraction(bindingSet);
		return this.answerFuture.handle((r, e) -> {

			if (r == null) {
				LOG.error("An exception has occured while executing Ask Interaction ", e);
				return null;
			} else {
				return r;
			}
		});
	}

	@Override
	void planPostInteraction(MyKnowledgeInteractionInfo aPKI) {
		this.myKnowledgeInteraction = aPKI;
	}

	@Override
	CompletableFuture<PostResult> executePostInteraction(BindingSet bindingSet) {
		this.LOG.trace("processPost()");
		this.reactFuture = new CompletableFuture<>();
		this.checkOtherKnowledgeInteraction(bindingSet);
		return this.reactFuture.handle((r, e) -> {

			if (r == null) {
				LOG.error("An exception has occured while executing Post Interaction ", e);
				return null;
			} else {
				return r;
			}
		});
	}

	private void checkOtherKnowledgeInteraction(BindingSet bindingSet) {

		synchronized (this.lock) {
			if (this.kiIter.hasNext()) {
				KnowledgeInteractionInfo ki = this.kiIter.next();
				if (this.myKnowledgeInteraction.getType() == Type.ASK && ki.getType() == Type.ANSWER) {
					AnswerKnowledgeInteraction answerKI = (AnswerKnowledgeInteraction) ki.getKnowledgeInteraction();
					AskKnowledgeInteraction askKI = (AskKnowledgeInteraction) this.myKnowledgeInteraction
							.getKnowledgeInteraction();
					if (this.matches(askKI.getPattern(), answerKI.getPattern())) {

						BindingSet transformedAskBindingSet = GraphPatternMatcher
								.transformBindingSet(askKI.getPattern(), answerKI.getPattern(), bindingSet);

						AskMessage askMessage = new AskMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(),
								transformedAskBindingSet);
						try {
							this.answerMessageFuture = this.messageRouter.sendAskMessage(askMessage);
							this.previousSend = Instant.now();
							this.answerMessageFuture.thenAccept(aMessage -> {
								try {
									this.answerMessageFuture = null;

									BindingSet transformedAnswerBindingSet = GraphPatternMatcher.transformBindingSet(
											answerKI.getPattern(), askKI.getPattern(), aMessage.getBindings());

									// TODO make sure there are no duplicates
									this.allBindings.addAll(transformedAnswerBindingSet);

									this.askExchangeInfos
											.add(convertMessageToExchangeInfo(aMessage.getBindings(), aMessage));

									this.checkOtherKnowledgeInteraction(bindingSet);
								} catch (Throwable t) {
									this.LOG.error("Receiving an answer message should succeed.", t);
								}
							});
						} catch (IOException e) {
							this.LOG.warn("An error occured while sending and processing a message: {}", e);
							this.LOG.debug("This is the message where the error occured: {}", askMessage);

							// continue with the work, otherwise this process will come to a halt.
							this.checkOtherKnowledgeInteraction(bindingSet);
						}
					} else {
						this.checkOtherKnowledgeInteraction(bindingSet);
					}
				} else if (this.myKnowledgeInteraction.getType() == Type.POST && ki.getType() == Type.REACT) {

					ReactKnowledgeInteraction rKI = (ReactKnowledgeInteraction) ki.getKnowledgeInteraction();

					PostKnowledgeInteraction pKI = (PostKnowledgeInteraction) this.myKnowledgeInteraction
							.getKnowledgeInteraction();
					if (this.matches(pKI.getArgument(), rKI.getArgument())) {

						BindingSet transformedArgBindingSet = GraphPatternMatcher.transformBindingSet(pKI.getArgument(),
								rKI.getArgument(), bindingSet);

						PostMessage postMessage = new PostMessage(this.myKnowledgeInteraction.getKnowledgeBaseId(),
								this.myKnowledgeInteraction.getId(), ki.getKnowledgeBaseId(), ki.getId(),
								transformedArgBindingSet);
						try {
							this.reactMessageFuture = this.messageRouter.sendPostMessage(postMessage);
							this.previousSend = Instant.now();
							this.reactMessageFuture.thenAccept(aMessage -> {
								try {
									this.reactMessageFuture = null;

									BindingSet transformedResultBindingSet = new BindingSet();
									if (pKI.getResult() != null) {
										assert rKI.getResult() != null;

										transformedResultBindingSet = GraphPatternMatcher.transformBindingSet(
												rKI.getResult(), pKI.getResult(), aMessage.getResult());
										// TODO make sure there are no duplicates
										this.allBindings.addAll(transformedResultBindingSet);
									}

									this.postExchangeInfos.add(convertMessageToExchangeInfo(transformedArgBindingSet,
											aMessage.getResult(), aMessage));
									// TODO should this statement be moved outside this try/catch, since it cannot
									// throw an exception and it has nothing to do with receiving a message.
									this.checkOtherKnowledgeInteraction(bindingSet);
								} catch (Throwable t) {
									this.LOG.error("Receiving a react message should succeed.", t);
								}
							});
						} catch (IOException e) {
							this.LOG.warn("An error occured while sending and processing a message: {}", e);
							this.LOG.debug("This is the message where the error occured: {}", postMessage);

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
				LOG.trace("Exchanged data with the following {} KBs:", this.askExchangeInfos.size());
				if (this.answerFuture != null) {
					this.answerFuture.complete(new AskResult(this.allBindings, this.askExchangeInfos));
				} else if (this.reactFuture != null) {
					this.reactFuture.complete(new PostResult(this.allBindings, this.postExchangeInfos));
				}
			}
		}
	}

	private AskExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedBindings, AnswerMessage aMessage) {
		Status status = Status.SUCCEEDED;
		String failedMessage = aMessage.getFailedMessage();

		if (failedMessage != null) {
			status = Status.FAILED;
		}

		return new AskExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedBindings, this.previousSend, Instant.now(), status,
				failedMessage);
	}

	private PostExchangeInfo convertMessageToExchangeInfo(BindingSet someConvertedArgumentBindings,
			BindingSet someConvertedResultBindings, ReactMessage aMessage) {
		Status status = Status.SUCCEEDED;
		String failedMessage = aMessage.getFailedMessage();

		if (failedMessage != null) {
			status = Status.FAILED;
		}

		return new PostExchangeInfo(Initiator.KNOWLEDGEBASE, aMessage.getFromKnowledgeBase(),
				aMessage.getFromKnowledgeInteraction(), someConvertedArgumentBindings, someConvertedResultBindings,
				this.previousSend, Instant.now(), status, failedMessage);
	}

	private boolean matches(GraphPattern gp1, GraphPattern gp2) {
		try {
			return GraphPatternMatcher.areIsomorphic(gp1, gp2);
		} catch (ParseException e) {
			LOG.error("Graph pattern parsing should succeed.", e);
		}
		return false;
	}

}
