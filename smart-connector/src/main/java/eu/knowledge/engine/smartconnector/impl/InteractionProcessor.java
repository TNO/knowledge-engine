package eu.knowledge.engine.smartconnector.impl;

import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;
import eu.knowledge.engine.smartconnector.api.ReactHandler;
import eu.knowledge.engine.smartconnector.api.RecipientSelector;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;

/**
 * /** Reactive responsibilities: The {@link InteractionProcessor} receives
 * {@link AskMessage} and {@link PostMessage} objects, and is responsible for
 * processing these into respectively {@link AnswerMessage} and
 * {@link ReactMessage} objects.
 *
 * For this, it needs to know which knowledge interactions are offered by the
 * knowledge base that this smart connector is attached to. For this, it uses
 * {@link KnowledgeBaseStore}, and also {@link MyMetaKnowledgeBase} for the
 * knowledge interactions about the metadata that all smart connectors
 * automatically offer.
 *
 * Proactive responsibilities:
 * <ul>
 * <li>handle all proactive interactions like {@link AskKnowledgeInteraction}
 * and {@link PostKnowledgeInteraction}</li>
 * <li>determine the potential {@link KnowledgeBase}s for which matching
 * {@link KnowledgeInteraction}s should be found</li>
 * <li>reduce the potential {@link KnowledgeBase}s to the ones that actually
 * have compatible {@link KnowledgeInteraction}s</li>
 * <li>Send the {@link KnowledgeBase}s the correct messages via the message
 * dispatcher interface.</li>
 * <li>process the results from other {@link KnowledgeBase}s</li>
 * <li>contact the {@link KnowledgeBase} via the {@link CompletableFuture}
 * interface</li>
 * <li>take into account that the reasoner will be added in a future
 * version</li>
 * <li></li>
 * </ul>
 */
public interface InteractionProcessor {

	/**
	 * Process an {@link AskKnowledgeInteraction} from MyKnowledgeBase.
	 *
	 * @param anAKI       The {@link AskKnowledgeInteraction} to process.
	 * @param aSelector   The {@link RecipientSelector} to limit the
	 *                    OtherKnowledgeBases who's
	 *                    {@link AnswerKnowledgeInteraction} will be called.
	 * @param aBindingSet The {@link BindingSet} containing limitations on the
	 *                    expected answers. The variable names in the bindings
	 *                    should occur in the {@link GraphPattern} of the
	 *                    {@link AskKnowledgeInteraction}.
	 * @return A future to an {@link AskResult}. This means this method immediately
	 *         returns and will continue processing the
	 *         {@link AskKnowledgeInteraction} in the background. Once the
	 *         processing is done, the future can be used to retrieve the
	 *         {@link AskResult} and access its {@link BindingSet}.
	 */
	CompletableFuture<AskResult> processAskFromKnowledgeBase(MyKnowledgeInteractionInfo anAKI,
			RecipientSelector aSelector, BindingSet aBindingSet);

	/**
	 * Process an {@link PostKnowledgeInteraction} from MyKnowledgeBase.
	 *
	 * @param aPKI          The {@link PostKnowledgeInteraction} to process.
	 * @param aSelector     The {@link RecipientSelector} to limit the
	 *                      OtherKnowledgeBases who's
	 *                      {@link ReactKnowledgeInteraction} will be called.
	 * @param aBindingSet The {@link BindingSet} containing limitations on the
	 *                      expected answers. The variable names in the bindings
	 *                      should occur in the {@link GraphPattern} of the
	 *                      {@link PostKnowledgeInteraction}.
	 * @return A future to an {@link AskResult}. This means this method immediately
	 *         returns and will continue processing the
	 *         {@link PostKnowledgeInteraction} in the background. Once the
	 *         processing is done, the future can be used to retrieve the
	 *         {@link PostResult} and access its {@link BindingSet}.
	 */
	CompletableFuture<PostResult> processPostFromKnowledgeBase(MyKnowledgeInteractionInfo aPKI, RecipientSelector aSelector, BindingSet aBindingSet);

	/**
	 * Interprets the given {@link AskMessage} and returns an {@link AnswerMessage}
	 * by delegating the {@link BindingSet} to the correct {@link AnswerHandler}, OR
	 * to a handler in {@link MyMetaKnowledgeBase} if the incoming message asks for
	 * metadata about this knowledge base.
	 *
	 * @param anAskMsg The {@link AskMessage} that requires an answer.
	 * @return A future {@link AnswerMessage}.
	 */
	CompletableFuture<AnswerMessage> processAskFromMessageRouter(AskMessage askMessage);

	/**
	 * Interprets the given {@link PostMessage} and returns a {@link ReactMessage}
	 * by delegating the {@link BindingSet} to the correct {@link ReactHandler}, OR
	 * to a handler in {@link OtherKnowledgeBaseStore} if it concerns metadata about
	 * other knowledge bases.
	 *
	 * @param aPostMsg The {@link PostMessage} that requires a reaction.
	 * @return A future {@link ReactMessage}.
	 */
	CompletableFuture<ReactMessage> processPostFromMessageRouter(PostMessage postMessage);

	void setMessageRouter(MessageRouter messageRouter);

	void unsetMessageRouter();
}
