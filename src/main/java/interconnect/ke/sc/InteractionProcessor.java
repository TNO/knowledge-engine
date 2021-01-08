package interconnect.ke.sc;

import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.RecipientSelector;
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

/**
 * /** Reactive responsibilities: The {@link InteractionProcessor} receives
 * {@link AskMessage} and {@link PostMessage} objects, and is responsible for
 * processing these into respectively {@link AnswerMessage} and
 * {@link ReactMessage} objects.
 * 
 * For this, it needs to know which knowledge interactions are offered by the
 * knowledge base that this smart connector is attached to. For this, it uses
 * {@link MyKnowledgeBaseStore}, and also {@link MyMetaKnowledgeBase} for the
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
	 *         {@link AskKnowledgeInteraction} in the background. Ones the
	 *         processing is done, the future can be used to retrieve the
	 *         {@link AskResult} and access its {@link BindingSet}.
	 */
	CompletableFuture<AskResult> processAskFromKnowledgeBase(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet);

	/**
	 * Process an {@link PostKnowledgeInteraction} from MyKnowledgeBase.
	 * 
	 * @param aPKI          The {@link PostKnowledgeInteraction} to process.
	 * @param aSelector     The {@link RecipientSelector} to limit the
	 *                      OtherKnowledgeBases who's
	 *                      {@link ReactKnowledgeInteraction} will be called.
	 * @param someArguments The {@link BindingSet} containing limitations on the
	 *                      expected answers. The variable names in the bindings
	 *                      should occur in the {@link GraphPattern} of the
	 *                      {@link PostKnowledgeInteraction}.
	 * @return A future to an {@link AskResult}. This means this method immediately
	 *         returns and will continue processing the
	 *         {@link PostKnowledgeInteraction} in the background. Ones the
	 *         processing is done, the future can be used to retrieve the
	 *         {@link PostResult} and access its {@link BindingSet}.
	 */
//	CompletableFuture<PostResult> processPostFromKnowledgeBase(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
//			BindingSet someArguments);

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
