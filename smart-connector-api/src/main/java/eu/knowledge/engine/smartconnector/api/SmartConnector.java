package eu.knowledge.engine.smartconnector.api;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.Rule;

public interface SmartConnector {

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * will ask certain types of questions for which it would like an answer. This
	 * allows the {@link SmartConnector} to prepare.
	 *
	 * @param anAskKI The {@link AskKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to register with this
	 *                {@link SmartConnector}.
	 */
	URI register(AskKnowledgeInteraction anAskKI);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * no longer will ask certain types of questions. This allows the
	 * {@link SmartConnector} to prepare.
	 *
	 * @param anAskKI The {@link AskKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to unregister from this
	 *                {@link SmartConnector}.
	 */
	void unregister(AskKnowledgeInteraction anAskKI);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * can answer certain types of questions that other {@link KnowledgeBase}s would
	 * like to {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)}.
	 * This allows the {@link SmartConnector} to prepare.
	 *
	 * @param anAnswerKI     The {@link AskKnowledgeInteraction} that the
	 *                       {@link KnowledgeBase} wants to register with this
	 *                       {@link SmartConnector}.
	 * @param aAnswerHandler The {@link AnswerHandler} that will process and answer
	 *                       an incoming question from another
	 *                       {@link KnowledgeBase}.
	 */
	URI register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * no longer answers certain types of questions. This allows the
	 * {@link SmartConnector} to prepare.
	 *
	 * @param anAnswerKI The {@link AnswerKnowledgeInteraction} that the
	 *                   {@link KnowledgeBase} wants to unregister from this
	 *                   {@link SmartConnector}.
	 */
	void unregister(AnswerKnowledgeInteraction anAnswerKI);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * will post certain type of data in which other {@link KnowledgeBase}s might
	 * want to react. This allows the {@link SmartConnector} to prepare.
	 *
	 * @param aPostKI The {@link PostKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to register with this
	 *                {@link SmartConnector}.
	 */
	URI register(PostKnowledgeInteraction aPostKI);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * will no longer post certain types of data. This allows the
	 * {@link SmartConnector} to prepare.
	 *
	 * @param aPostKI The {@link PostKnowledgeInteraction} that the
	 *                {@link KnowledgeBase} wants to unregister from this
	 *                {@link SmartConnector}.
	 */
	void unregister(PostKnowledgeInteraction aPostKI);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * wants to react to certain types of data that other {@link KnowledgeBase}s
	 * will {@link #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)}.
	 * This allows the {@link SmartConnector} to prepare.
	 *
	 * @param anReactKI     The {@link AskKnowledgeInteraction} that the
	 *                      {@link KnowledgeBase} wants to register with this
	 *                      {@link SmartConnector}.
	 * @param aReactHandler The {@link AnswerHandler} that will process and answer
	 *                      an incoming question from another {@link KnowledgeBase}.
	 */
	URI register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler);

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * no longer reacts to certain types of data. This allows the
	 * {@link SmartConnector} to prepare.
	 *
	 * @param anReactKI The {@link ReactKnowledgeInteraction} that the
	 *                  {@link KnowledgeBase} wants to unregister from this
	 *                  {@link SmartConnector}.
	 */
	void unregister(ReactKnowledgeInteraction anReactKI);

	/**
	 * Return a plan for executing an ask knowledge interaction. This plan can be
	 * executed using {@link AskPlan#execute(BindingSet)}.
	 * 
	 * @return
	 */
	AskPlan planAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector);

	/**
	 * With this method a {@link KnowledgeBase} can ask a question to its
	 * {@link SmartConnector}. The Smart Connector will first check which of all the
	 * other {@link KnowledgeBase}s fit the {@link RecipientSelector} and
	 * subsequently determine whether those that fit the selector have a compatible
	 * or matching {@link AnswerKnowledgeInteraction}. The resulting other
	 * {@link KnowledgeBase}s will have their matching
	 * {@link AnswerKnowledgeInteraction}'s {@link AnswerHandler} triggered. If
	 * there are multiple matching {@link KnowledgeBase}s this
	 * {@link SmartConnector} will combine their results.
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
	 *                    registered with the {@link SmartConnector} via the
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
	 *                    {@link AskKnowledgeInteraction}. Cannot be null!
	 * @return A {@link CompletableFuture} that will return a {@link AskResult} in
	 *         the future when the question is successfully processed by the
	 *         {@link SmartConnector}.
	 */
	CompletableFuture<AskResult> ask(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet);

	/**
	 * Performs an
	 * {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)} with a
	 * wildcard {@link RecipientSelector}. This means that all
	 * {@link KnowledgeBase}s that have matching {@link KnowledgeInteraction}s are
	 * allowed to answer the question being asked. This is the most interoperable
	 * way in using the {@link SmartConnector}, because it allows any compatible
	 * {@link KnowledgeBase} to join the data exchange.
	 *
	 * @see SmartConnector#ask(AskKnowledgeInteraction, RecipientSelector,
	 *      BindingSet)
	 * @see SmartConnector#planAsk(AskKnowledgeInteraction, RecipientSelector,
	 *      BindingSet)
	 */
	CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings);

	/**
	 * Returns a plan for executing a post knowledge interaction. This plan can be
	 * executed using {@link PostPlan#execute(BindingSet)}.
	 * 
	 * @param aPKI
	 * @param aSelector
	 * @return
	 */
	PostPlan planPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector);

	/**
	 * With this method a {@link KnowledgeBase} can post data to its
	 * {@link SmartConnector}. The Smart Connector will first check which of all the
	 * other {@link KnowledgeBase}s fit the {@link RecipientSelector} and
	 * subsequently determine whether those that fit the selector have a compatible
	 * or matching {@link ReactKnowledgeInteraction}. The resulting other
	 * {@link KnowledgeBase}s will have their matching
	 * {@link ReactKnowledgeInteraction}'s {@link ReactHandler} triggered. If there
	 * are multiple matching {@link KnowledgeBase}s this {@link SmartConnector} will
	 * allow all of them to react.
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
	 *                      registered with the {@link SmartConnector} via the
	 *                      {@link #register(AskKnowledgeInteraction)} method.
	 * @param aSelector     A selector that allows the {@link KnowledgeBase} to
	 *                      limit the potential recipients that can answer the
	 *                      question. It can be either a specific
	 *                      {@link KnowledgeBase#getKnowledgeBaseId()}, a complete
	 *                      wildcard or something in between where potential
	 *                      {@link KnowledgeBase} recipients are selected based on
	 *                      criteria from the KnowledgeBase ontology. Cannot be
	 *                      null!
	 * @param someArguments Allows the calling {@link KnowledgeBase} to limit the
	 *                      question to specific values for specific variables from
	 *                      the {@link GraphPattern} in the
	 *                      {@link AskKnowledgeInteraction}.
	 * @return A {@link CompletableFuture} that will return a {@link PostResult} in
	 *         the future when the post is successfully processed by the
	 *         {@link SmartConnector}.
	 * 
	 * @see SmartConnector#planPost(PostKnowledgeInteraction, RecipientSelector,
	 *      BindingSet)
	 */
	CompletableFuture<PostResult> post(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments);

	/**
	 * Performs an
	 * {@link #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)} with a
	 * wildcard {@link RecipientSelector}. This means that all
	 * {@link KnowledgeBase}s that have matching {@link KnowledgeInteraction}s are
	 * allowed to answer the question being asked. This is the most interoperable
	 * way in using the {@link SmartConnector}, because it allows any compatible
	 * {@link KnowledgeBase} to join the data exchange.
	 *
	 * @see #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)
	 */
	CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument);

	/**
	 * Sets the domain knowledge of this smart connector. This domain knowledge will
	 * be taken into account when the reasoner orchestrates the knowledge
	 * interactions. Note that by default there is no domain knowledge and setting
	 * it will overwrite the existing set of rules. The domain knowledge is only
	 * used when the reasoner is enabled. See
	 * {@link SmartConnector#setReasonerEnabled(boolean)}.
	 * 
	 * @param someRules The rules to take into account.
	 */
	void setDomainKnowledge(Set<Rule> someDomainKnowledge);

	/**
	 * Sets the default reasoner level of this Smart Connector between 1-5.
	 * Increasing the level of the reasoner causes the data exchange to become more
	 * flexible, but also causes the data exchange to be slower.
	 * 
	 * @param aReasonerLevel The default level of the reasoner. For details on the
	 *                       different reasoner levels, see
	 *                       {@link SmartConnectorConfig#CONF_KEY_KE_REASONER_LEVEL}.
	 */
	void setReasonerLevel(int aReasonerLevel);

	/**
	 * @return The default reasoner level of this smart connector for data exchange.
	 *         For details on the different reasoner levels, see
	 *         {@link SmartConnectorConfig#CONF_KEY_KE_REASONER_LEVEL}
	 */
	int getReasonerLevel();

	/**
	 * Stops the current {@link SmartConnector}. Note that this methods is
	 * asynchronous and will call
	 * {@link KnowledgeBase#smartConnectorStopped(SmartConnector)} when this smart
	 * connector has successfully stopped.
	 *
	 * After it has stopped, the {@link SmartConnector} can no longer be used by its
	 * {@link KnowledgeBase} to exchange data and the {@link KnowledgeBase} itself
	 * is no longer available to other {@link KnowledgeBase} for interoperable data
	 * exchange.
	 *
	 * Between calling this method and having the
	 * {@link KnowledgeBase#smartConnectorStopped(SmartConnector)} method called,
	 * its methods should not be called and the behaviour of the
	 * {@link SmartConnector} is unpredictable.
	 *
	 * Note that a stopped {@link SmartConnector} can no longer be used.
	 */
	void stop();

}