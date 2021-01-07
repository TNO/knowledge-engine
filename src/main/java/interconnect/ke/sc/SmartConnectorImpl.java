package interconnect.ke.sc;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.AnswerHandler;
import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.ReactHandler;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.messaging.SmartConnectorEndpoint;
import interconnect.ke.runtime.SmartConnectorRegistryImpl;

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
public class SmartConnectorImpl implements SmartConnector, SmartConnectorEndpoint {

	private static final Logger LOG = LoggerFactory.getLogger(SmartConnectorImpl.class);

	private final KnowledgeBase myKnowledgeBase;
	private final MyKnowledgeBaseStore myKnowledgeBaseStore;
	// private final MyMetaKnowledgeBase
	private final ReactiveInteractionProcessor reactiveInteractionProcessor;
	private final ProactiveInteractionProcessor proactiveInteractionProcessor;
	private final OtherKnowledgeBaseStore otherKnowledgeBaseStore;
	private MessageDispatcherEndpoint messageDispatcherEndpoint;

	/**
	 * Create a {@link SmartConnectorImpl}
	 * 
	 * @param aKnowledgeBase The {@link KnowledgeBase} this smart connector belongs
	 *                       to.
	 */
	public SmartConnectorImpl(KnowledgeBase aKnowledgeBase) {
		myKnowledgeBase = aKnowledgeBase;
		this.myKnowledgeBaseStore = new MyKnowledgeBaseStoreImpl(this.myKnowledgeBase.getKnowledgeBaseId());
		reactiveInteractionProcessor = null; // TODO
		this.otherKnowledgeBaseStore = null; // TODO
		this.proactiveInteractionProcessor = new ProactiveInteractionProcessorImpl(this.otherKnowledgeBaseStore);
		SmartConnectorRegistryImpl.getInstance().register(this);
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
	 */
	@Override
	public void register(AskKnowledgeInteraction anAskKI) {
		this.myKnowledgeBaseStore.register(anAskKI);
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
		this.myKnowledgeBaseStore.unregister(anAskKI);
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
	 */
	@Override
	public void register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler anAnswerHandler) {
		this.myKnowledgeBaseStore.register(anAnswerKI, anAnswerHandler);
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
		this.myKnowledgeBaseStore.unregister(anAnswerKI);
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
	 */
	@Override
	public void register(PostKnowledgeInteraction aPostKI) {
		this.myKnowledgeBaseStore.register(aPostKI);
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
		this.myKnowledgeBaseStore.unregister(aPostKI);
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
	 */
	@Override
	public void register(ReactKnowledgeInteraction aReactKI, ReactHandler aReactHandler) {
		this.myKnowledgeBaseStore.register(aReactKI, aReactHandler);
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
		this.myKnowledgeBaseStore.unregister(aReactKI);
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
	 *                    criteria from the KnowledgeBase ontology.
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
		return this.proactiveInteractionProcessor.processAsk(anAKI, aSelector, aBindingSet);
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
		return ask(ki, null, bindings);
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
	 *                      criteria from the KnowledgeBase ontology.
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
		return CompletableFuture.supplyAsync(() -> null);
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
		return post(ki, null, argument);
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
		SmartConnectorRegistryImpl.getInstance().unregister(this);
		this.myKnowledgeBase.smartConnectorStopped(this);
	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.myKnowledgeBase.getKnowledgeBaseId();
	}

	@Override
	public void handleAskMessage(AskMessage message) {
		this.reactiveInteractionProcessor.processAsk(message).thenAccept((m) -> {
			try {
				this.messageDispatcherEndpoint.send(m);
			} catch (IOException e) {
				LOG.error("The message '" + m + "' should send correctly.", e);
			}
		});
	}

	@Override
	public void handleAnswerMessage(AnswerMessage message) {
		this.proactiveInteractionProcessor.handleAnswerMessage(message);
	}

	@Override
	public void handlePostMessage(PostMessage message) {
		this.reactiveInteractionProcessor.processPost(message).thenAccept((m) -> {
			try {
				this.messageDispatcherEndpoint.send(m);
			} catch (IOException e) {
				LOG.error("The message '" + m + "' should send correctly.", e);
			}
		});
	}

	@Override
	public void handleReactMessage(ReactMessage message) {
		this.proactiveInteractionProcessor.handleReactMessage(message);
	}

	@Override
	public void setMessageDispatcher(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		this.messageDispatcherEndpoint = messageDispatcherEndpoint;
		this.proactiveInteractionProcessor.setMessageDispatcherEndpoint(this.messageDispatcherEndpoint);
		this.myKnowledgeBase.smartConnectorReady(this); // TODO second time the MessageDispatcher is called, it should
														// call the connectionRestored method.

	}

	@Override
	public void unsetMessageDispatcher() {
		this.messageDispatcherEndpoint = null;
		this.proactiveInteractionProcessor.unsetMessageDispatcherEndpoint();
	}

}
