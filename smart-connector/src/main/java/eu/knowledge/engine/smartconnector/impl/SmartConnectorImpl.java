package eu.knowledge.engine.smartconnector.impl;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.BindingValidator;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactHandler;
import eu.knowledge.engine.smartconnector.api.ReactKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.impl.KnowledgeInteractionInfo.Type;
import eu.knowledge.engine.smartconnector.messaging.SmartConnectorEndpoint;
import eu.knowledge.engine.smartconnector.runtime.KeRuntime;

/**
 * The {@link SmartConnectorImpl} is the main component of the KnowledgeEngine.
 * It's function is to facilitate the {@link KnowledgeBase} with interoperable
 * data exchange with other {@link KnowledgeBase}s. This is done by registering
 * four types of {@link KnowledgeInteraction}s beforehand that indicate the
 * capabilities of the {@link KnowledgeBase}.
 *
 * After registration the
 * {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)} and
 * {@link #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)}
 * methods can be used by the {@link KnowledgeBase} to proactively exchange
 * data, while the {@link AnswerHandler} and {@link ReactHandler} are used to
 * reactively exchange data.
 */
public class SmartConnectorImpl implements RuntimeSmartConnector, LoggerProvider {
	private final Logger LOG;

	private final KnowledgeBase myKnowledgeBase;
	private final KnowledgeBaseStore knowledgeBaseStore;
	private final MetaKnowledgeBase metaKnowledgeBase;
	private final InteractionProcessor interactionProcessor;
	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private final MessageRouterImpl messageRouter;
	private boolean isStopped = false;
	private final boolean knowledgeBaseIsThreadSafe;
	private final ExecutorService knowledgeBaseExecutorService;
	private final BindingValidator bindingValidator = new BindingValidator();
	private CompletableFuture<Void> constructorFinished = new CompletableFuture<Void>();
	private Instant started;

	/**
	 * Create a {@link SmartConnectorImpl}
	 *
	 * @param aKnowledgeBase The {@link KnowledgeBase} this smart connector belongs
	 *                       to.
	 */
	public SmartConnectorImpl(KnowledgeBase aKnowledgeBase, boolean knowledgeBaseIsThreadSafe) {
		this.started = Instant.now();
		this.myKnowledgeBase = aKnowledgeBase;

		this.LOG = this.getLogger(SmartConnectorImpl.class);

		this.knowledgeBaseStore = new KnowledgeBaseStoreImpl(this, this.myKnowledgeBase);
		this.messageRouter = new MessageRouterImpl(this);
		this.metaKnowledgeBase = new MetaKnowledgeBaseImpl(this, this.messageRouter, this.knowledgeBaseStore);
		this.otherKnowledgeBaseStore = new OtherKnowledgeBaseStoreImpl(this, this.metaKnowledgeBase);
		this.interactionProcessor = new InteractionProcessorImpl(this, this.otherKnowledgeBaseStore,
				this.knowledgeBaseStore);

		this.metaKnowledgeBase.setOtherKnowledgeBaseStore(this.otherKnowledgeBaseStore);
		this.metaKnowledgeBase.setInteractionProcessor(this.interactionProcessor);
		this.interactionProcessor.setMessageRouter(this.messageRouter);
		this.messageRouter.registerMetaKnowledgeBase(this.metaKnowledgeBase);
		this.messageRouter.registerInteractionProcessor(this.interactionProcessor);

		this.knowledgeBaseIsThreadSafe = knowledgeBaseIsThreadSafe;
		this.knowledgeBaseExecutorService = knowledgeBaseIsThreadSafe ? KeRuntime.executorService()
				: Executors.newSingleThreadExecutor();

		KeRuntime.localSmartConnectorRegistry().register(this);

		this.constructorFinished.complete(null);
	}

	@Override
	public Logger getLogger(Class<?> class1) {
		return LoggerFactory.getLogger(class1.getSimpleName() + "-" + this.myKnowledgeBase.getKnowledgeBaseName());
	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.myKnowledgeBase.getKnowledgeBaseId();
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it will ask certain types of questions for which it would like an
	 * answer. This allows the {@link SmartConnectorImpl} to prepare.
	 *
	 * @param anAskKI The {@link AskKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to register with this
	 *                {@link SmartConnectorImpl}.
	 * @return
	 */
	@Override
	public URI register(AskKnowledgeInteraction anAskKI) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.register(anAskKI, false);
		LOG.info("Registered Ask KI <{}>.", kiId);
		return kiId;
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it no longer will ask certain types of questions. This allows the
	 * {@link SmartConnectorImpl} to prepare.
	 *
	 * @param anAskKI The {@link AskKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to unregister from this
	 *                {@link SmartConnectorImpl}.
	 */
	@Override
	public void unregister(AskKnowledgeInteraction anAskKI) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.getKnowledgeInteractionByObject(anAskKI).getId();
		this.knowledgeBaseStore.unregister(anAskKI);
		LOG.info("Unregistered Ask KI <{}>.", kiId);
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it can answer certain types of questions that other
	 * {@link KnowledgeBase}s would like to
	 * {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)}. This
	 * allows the {@link SmartConnectorImpl} to prepare.
	 *
	 * @param anAnswerKI      The {@link AskKnowledgeInteraction} that the
	 *                        {@link KnowledgeBase} wants to register with this
	 *                        {@link SmartConnectorImpl}.
	 * @param anAnswerHandler The {@link AnswerHandler} that will process and answer
	 *                        an incoming question from another
	 *                        {@link KnowledgeBase}.
	 * @return
	 */
	@Override
	public URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler anAnswerHandler) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.register(anAnswerKI, anAnswerHandler, false);
		LOG.info("Registered Answer KI <{}>.", kiId);
		return kiId;
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it no longer answers certain types of questions. This allows the
	 * {@link SmartConnectorImpl} to prepare.
	 *
	 * @param anAnswerKI The {@link AswerKnowledgeInteraction} that the
	 *                   {@link KnowledgeBase} wants to unregister from this
	 *                   {@link SmartConnectorImpl}.
	 */
	@Override
	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.getKnowledgeInteractionByObject(anAnswerKI).getId();
		this.knowledgeBaseStore.unregister(anAnswerKI);
		LOG.info("Unregistered Answer KI <{}>.", kiId);
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it will post certain type of data in which other {@link KnowledgeBase}s
	 * might want to react. This allows the {@link SmartConnectorImpl} to prepare.
	 *
	 * @param aPostKI The {@link PostKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to register with this
	 *                {@link SmartConnectorImpl}.
	 * @return
	 */
	@Override
	public URI register(PostKnowledgeInteraction aPostKI) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.register(aPostKI, false);
		LOG.info("Registered Post KI <{}>.", kiId);
		return kiId;
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it will no longer post certain types of data. This allows the
	 * {@link SmartConnectorImpl} to prepare.
	 *
	 * @param aPostKI The {@link PostKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to unregister from this
	 *                {@link SmartConnectorImpl}.
	 */
	@Override
	public void unregister(PostKnowledgeInteraction aPostKI) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.getKnowledgeInteractionByObject(aPostKI).getId();
		this.knowledgeBaseStore.unregister(aPostKI);
		LOG.info("Unregistered Post KI <{}>.", kiId);
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it wants to react to certain types of data that other
	 * {@link KnowledgeBase}s will
	 * {@link #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)}. This
	 * allows the {@link SmartConnectorImpl} to prepare.
	 *
	 * @param aReactKI      The {@link AskKnowledgeInteraction} that the
	 *                      {@link KnowledgeBase} wants to register with this
	 *                      {@link SmartConnectorImpl}.
	 * @param aReactHandler The {@link AnswerHandler} that will process and answer
	 *                      an incoming question from another {@link KnowledgeBase}.
	 * @return
	 */
	@Override
	public URI register(ReactKnowledgeInteraction aReactKI, ReactHandler aReactHandler) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.register(aReactKI, aReactHandler, false);
		LOG.info("Registered React KI <{}>.", kiId);
		return kiId;
	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnectorImpl} (and via it other {@link KnowledgeBase}s) know
	 * that it no longer reacts to certain types of data. This allows the
	 * {@link SmartConnectorImpl} to prepare.
	 *
	 * @param aReactKI The {@link ReactKnowledgeInteraction} that the
	 *                 {@link KnowledgeBase} wants to unregister from this
	 *                 {@link SmartConnectorImpl}.
	 */
	@Override
	public void unregister(ReactKnowledgeInteraction aReactKI) {
		this.checkStopped();
		URI kiId = this.knowledgeBaseStore.getKnowledgeInteractionByObject(aReactKI).getId();
		this.knowledgeBaseStore.unregister(aReactKI);
		LOG.info("Unregistered React KI <{}>.", kiId);
	}

	/**
	 * With this method a {@link KnowledgeBase} can ask a question to its
	 * {@link SmartConnectorImpl}. The Smart Connector will first check which of all
	 * the other {@link KnowledgeBase}s fit the {@link RecipientSelector} and
	 * subsequently determine whether those that fit the selector have a compatible
	 * or matching {@link AnswerKnowledgeInteraction}. The resulting other
	 * {@link KnowledgeBase}s will have their matching
	 * {@link AnswerKnowledgeInteraction}'s {@link AnswerHandler} triggered. If
	 * there are multiple matching {@link KnowledgeBase}s this
	 * {@link SmartConnectorImpl} will combine their results.
	 *
	 * Using the {@link BindingSet} argument the caller can limit the question being
	 * asked by providing one or more allowed values for the answers to certain
	 * variables in the {@link GraphPattern} of the {@link AskKnowledgeInteraction}.
	 * Note that the different bindings in the set form a disjunction.
	 *
	 * This method is asynchronous (and uses {@link CompletableFuture}s) because
	 * depending on the nature of the question and the availability of the answer it
	 * might take a while.
	 *
	 * @param anAKI       The given {@link AskKnowledgeInteraction} should be
	 *                    registered with the {@link SmartConnectorImpl} via the
	 *                    {@link #register(AskKnowledgeInteraction)} method.
	 * @param aSelector   A selector that allows the {@link KnowledgeBase} to limit
	 *                    the potential recipients that can answer the question. It
	 *                    can be either a specific
	 *                    {@link KnowledgeBase#getKnowledgeBaseId()}, a complete
	 *                    wildcard or something in between where potential
	 *                    {@link KnowledgeBase} recipients are selected based on
	 *                    criteria from the KnowledgeBase ontology. Cannot be
	 *                    {@code null}.
	 * @param aBindingSet Allows the calling {@link KnowledgeBase} to limit the
	 *                    question to specific values for specific variables from
	 *                    the {@link GraphPattern} in the
	 *                    {@link AskKnowledgeInteraction}.
	 * @return A {@link CompletableFuture} that will return a {@link AskResult} in
	 *         the future when the question is successfully processed by the
	 *         {@link SmartConnectorImpl}.
	 */
	@Override
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {

		this.checkStopped();
		if (aSelector == null)
			throw new IllegalArgumentException("Recipient Selector parameter should be non-null.");

		MyKnowledgeInteractionInfo info = this.knowledgeBaseStore.getKnowledgeInteractionByObject(anAKI);

		this.bindingValidator.validatePartialBindings(anAKI.getPattern(), aBindingSet);

		assert info != null; // TODO omschrijven naar runtime check
		assert info.getType() == Type.ASK;

		LOG.info("Processing ask for KI <{}>.", info.getId());
		return this.interactionProcessor.processAskFromKnowledgeBase(info, aSelector, aBindingSet);
	}

	/**
	 * Performs an
	 * {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)} with a
	 * wildcard {@link RecipientSelector}. This means that all
	 * {@link KnowledgeBase}s that have matching {@link KnowledgeInteraction}s are
	 * allowed to answer the question being asked. This is the most interoperable
	 * way in using the {@link SmartConnectorImpl}, because it allows any compatible
	 * {@link KnowledgeBase} to join the data exchange.
	 *
	 * @see SmartConnectorImpl#ask(AskKnowledgeInteraction, RecipientSelector,
	 *      BindingSet)
	 */
	@Override
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings) {
		return this.ask(ki, new RecipientSelector(), bindings);
	}

	/**
	 * With this method a {@link KnowledgeBase} can post data to its
	 * {@link SmartConnectorImpl}. The Smart Connector will first check which of all
	 * the other {@link KnowledgeBase}s fit the {@link RecipientSelector} and
	 * subsequently determine whether those that fit the selector have a compatible
	 * or matching {@link ReactKnowledgeInteraction}. The resulting other
	 * {@link KnowledgeBase}s will have their matching
	 * {@link ReactKnowledgeInteraction}'s {@link ReactHandler} triggered. If there
	 * are multiple matching {@link KnowledgeBase}s this {@link SmartConnectorImpl}
	 * will allow all of them to react.
	 *
	 * This type of interaction can be used to make interoperable publish/subscribe
	 * like mechanisms where the post is the publish and the react (without a
	 * {@code result} {@link GraphPattern}) is an on_message method.
	 *
	 * Also, this type of interaction can be used to make an interoperable function.
	 * The {@link PostKnowledgeInteraction} being the one who calls the function
	 * (with the argument {@link GraphPattern} being the input parameters) and the
	 * {@link ReactKnowledgeInteraction} is the one who represents actual function
	 * implementation and provides a result (with result {@link GraphPattern} being
	 * the return value).
	 *
	 * Note that depending on whether the {@link ReactKnowledgeInteraction} being
	 * used has defined a {@code result} {@link GraphPattern}, there either is or is
	 * no data being returned.
	 *
	 * Using the {@link BindingSet} argument the caller should provide the actual
	 * data by providing one or more values for all the variables in the argument
	 * {@link GraphPattern} of the {@link PostKnowledgeInteraction}.
	 *
	 * This method is asynchronous (and uses {@link CompletableFuture}s) because
	 * depending on the nature of the post and the availability of results it might
	 * take a while.
	 *
	 * @param aPKI          The given {@link AskKnowledgeInteraction} should be
	 *                      registered with the {@link SmartConnectorImpl} via the
	 *                      {@link #register(AskKnowledgeInteraction)} method.
	 * @param aSelector     A selector that allows the {@link KnowledgeBase} to
	 *                      limit the potential recipients that can answer the
	 *                      question. It can be either a specific
	 *                      {@link KnowledgeBase#getKnowledgeBaseId()}, a complete
	 *                      wildcard or something in between where potential
	 *                      {@link KnowledgeBase} recipients are selected based on
	 *                      criteria from the KnowledgeBase ontology. Cannot be
	 *                      {@code null}.
	 * @param someArguments Allows the calling {@link KnowledgeBase} to limit the
	 *                      question to specific values for specific variables from
	 *                      the {@link GraphPattern} in the
	 *                      {@link AskKnowledgeInteraction}.
	 * @return A {@link CompletableFuture} that will return a {@link PostResult} in
	 *         the future when the post is successfully processed by the
	 *         {@link SmartConnectorImpl}.
	 */
	@Override
	public CompletableFuture<PostResult> post(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments) {
		this.checkStopped();

		if (aSelector == null)
			throw new IllegalArgumentException("Recipient Selector parameter should be non-null.");

		MyKnowledgeInteractionInfo info = this.knowledgeBaseStore.getKnowledgeInteractionByObject(aPKI);

		this.bindingValidator.validateCompleteBindings(aPKI.getArgument(), someArguments);

		assert info != null; // TODO omschrijven naar runtime check
		assert info.getType() == Type.POST;

		LOG.info("Processing post for KI <{}>.", info.getId());

		return this.interactionProcessor.processPostFromKnowledgeBase(info, aSelector, someArguments);
	}

	/**
	 * Performs an
	 * {@link #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)} with a
	 * wildcard {@link RecipientSelector}. This means that all
	 * {@link KnowledgeBase}s that have matching {@link KnowledgeInteraction}s are
	 * allowed to answer the question being asked. This is the most interoperable
	 * way in using the {@link SmartConnectorImpl}, because it allows any compatible
	 * {@link KnowledgeBase} to join the data exchange.
	 *
	 * @see #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)
	 */
	@Override
	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument) {
		return this.post(ki, new RecipientSelector(), argument);
	}

	/**
	 * Stops the current {@link SmartConnectorImpl}. Note that this methods is
	 * asynchronous and will call
	 * {@link KnowledgeBase#smartConnectorStopped(SmartConnectorImpl)} when this
	 * smart connector has successfully stopped.
	 *
	 * After it has stopped, the {@link SmartConnectorImpl} can no longer be used by
	 * its {@link KnowledgeBase} to exchange data and the {@link KnowledgeBase}
	 * itself is no longer available to other {@link KnowledgeBase} for
	 * interoperable data exchange.
	 *
	 * Between calling this method and having the
	 * {@link KnowledgeBase#smartConnectorStopped(SmartConnectorImpl)} method
	 * called, its methods should not be called and the behaviour of the
	 * {@link SmartConnectorImpl} is unpredictable.
	 *
	 * Note that a stopped {@link SmartConnectorImpl} can no longer be used.
	 */
	@Override
	public void stop() {
		this.checkStopped();

		this.isStopped = true;

		// this will trigger notifications to other Smart Connectors.
		this.knowledgeBaseStore.stop();

		this.otherKnowledgeBaseStore.stop();

		KeRuntime.localSmartConnectorRegistry().unregister(this);

		this.knowledgeBaseExecutorService.execute(() -> this.myKnowledgeBase.smartConnectorStopped(this));

		if (!this.knowledgeBaseIsThreadSafe) {
			// In that case this was a SingleThreadExecutor that was created in the
			// constructor specifically for this Knowledge Base
			this.knowledgeBaseExecutorService.shutdown();
		}
	}

	private void checkStopped() {
		if (this.isStopped) {
			throw new IllegalStateException("The SmartConnector cannot be used anymore once it is stopped");
		}
	}

	void communicationReady() {
		Instant beforePopulate = Instant.now();
		LOG.info("Getting comms ready took {} ms", Duration.between(this.started, beforePopulate).toMillis());
		// Populate the initial knowledge base store.
		this.otherKnowledgeBaseStore.populate().thenRun(() -> {
			LOG.info("Populating took {} ms", Duration.between(beforePopulate, Instant.now()).toMillis());
			Instant beforeAnnounce = Instant.now();
			// Then tell the other knowledge bases about our existence.
			this.metaKnowledgeBase.postNewKnowledgeBase().thenRun(() -> {
				LOG.info("Announcing took {} ms", Duration.between(beforeAnnounce, Instant.now()).toMillis());
				Instant beforeConstructorFinished = Instant.now();
				this.constructorFinished.thenRun(() -> {
					LOG.info("Constructor finished took {} ms",
							Duration.between(beforeConstructorFinished, Instant.now()).toMillis());
					// When that is done, and all peers have acknowledged our existence, we
					// can proceed to inform the knowledge base that this smart connector is
					// ready for action!
					this.knowledgeBaseExecutorService.execute(() -> {
						LOG.info("Ready to exchange data.");
						try {
							this.myKnowledgeBase.smartConnectorReady(this);
						} catch (Throwable t) {
							LOG.error("KnowledgeBase threw exception", t);
						}
					});
				});
			});
		});
	}

	void communicationInterrupted() {
		this.knowledgeBaseExecutorService.execute(() -> {
			try {
				this.myKnowledgeBase.smartConnectorConnectionLost(this);
			} catch (Throwable t) {
				LOG.error("KnowledgeBase threw exception", t);
			}
		});
	}

	void communicationRestored() {
		this.knowledgeBaseExecutorService.execute(() -> {
			try {
				this.myKnowledgeBase.smartConnectorConnectionRestored(this);
			} catch (Throwable t) {
				LOG.error("KnowledgeBase threw exception", t);
			}
		});
	}

	@Override
	public SmartConnectorEndpoint getSmartConnectorEndpoint() {
		return this.messageRouter;
	}

	@Override
	public void setDomainKnowledge(Set<Rule> someDomainKnowledge) {
		this.interactionProcessor.setDomainKnowledge(someDomainKnowledge);
	}
}
