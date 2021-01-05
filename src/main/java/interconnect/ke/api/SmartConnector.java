package interconnect.ke.api;

import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

/**
 * The {@link SmartConnector} is the main component of the KnowledgeEngine. It's
 * function is to facilitate the {@link KnowledgeBase} with interoperable data
 * exchange with other {@link KnowledgeBase}s. This is done by registering four
 * types of {@link KnowledgeInteraction}s beforehand that indicate the
 * capabilities of the {@link KnowledgeBase}.
 * 
 * After registration the
 * {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)} and
 * {@link #post(PostKnowledgeInteraction, RecipientSelector, BindingSet)}
 * methods can be used by the {@link KnowledgeBase} to proactively exchange
 * data, while the {@link AnswerHandler} and {@link ReactHandler} are used to
 * reactively exchange data.
 */
public class SmartConnector {

	private KnowledgeBase knowledgeBase;

	/**
	 * Create a {@link SmartConnector}
	 * 
	 * @param aKnowledgeBase The {@link KnowledgeBase} this smart connector belongs
	 *                       to.
	 */
	public SmartConnector(KnowledgeBase aKnowledgeBase) {
		knowledgeBase = aKnowledgeBase;
	}

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
	public void register(AskKnowledgeInteraction anAskKI) {

	}

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
	public void unregister(AskKnowledgeInteraction anAskKI) {

	}

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
	public void register(AnswerKnowledgeInteraction anAnswerKI, AnswerHandler aAnswerHandler) {

	}

	/**
	 * This method is used by the {@link KnowledgeBase} to let its
	 * {@link SmartConnector} (and via it other {@link KnowledgeBase}s) know that it
	 * no longer answers certain types of questions. This allows the
	 * {@link SmartConnector} to prepare.
	 * 
	 * @param anAnswerKI The {@link AswerKnowledgeInteraction} that the
	 *                   {@link KnowledgeBase} wants to unregister from this
	 *                   {@link SmartConnector}.
	 */
	public void unregister(AnswerKnowledgeInteraction anAnswerKI) {

	}

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
	public void register(PostKnowledgeInteraction aPostKI) {

	}

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
	public void unregister(PostKnowledgeInteraction aPostKI) {

	}

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
	public void register(ReactKnowledgeInteraction anReactKI, ReactHandler aReactHandler) {

	}

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
	public void unregister(ReactKnowledgeInteraction anReactKI) {

	}

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
	 * @param aAKI        The given {@link AskKnowledgeInteraction} should be
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
	 *                    {@link AskKnowledgeInteraction}.
	 * @return A {@link CompletableFuture} that will return a {@link AskResult} in
	 *         the future when the question is succesfully processed by the
	 *         {@link SmartConnector}.
	 */
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction aAKI, RecipientSelector aSelector,
			BindingSet aBindingSet) {
		return CompletableFuture.supplyAsync(() -> null);
	}

	/**
	 * Performs an
	 * {@link #ask(AskKnowledgeInteraction, RecipientSelector, BindingSet)} with a
	 * wildcard RecipientSelector meaning that all {@link KnowledgeBase}s that have
	 * matching {@link KnowledgeInteraction}s are allowed to answer the question
	 * being asked.
	 * 
	 * @see SmartConnector#ask(AskKnowledgeInteraction, RecipientSelector,
	 *      BindingSet)
	 */
	public CompletableFuture<AskResult> ask(AskKnowledgeInteraction ki, BindingSet bindings) {
		return ask(ki, null, bindings);
	}

	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, RecipientSelector r, BindingSet arguments) {
		return CompletableFuture.supplyAsync(() -> null);
	}

	public CompletableFuture<PostResult> post(PostKnowledgeInteraction ki, BindingSet argument) {
		return post(ki, null, argument);
	}

	public void stop() {

	}
}
