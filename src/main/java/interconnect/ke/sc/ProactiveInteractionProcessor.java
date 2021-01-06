package interconnect.ke.sc;

import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.GraphPattern;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AnswerKnowledgeInteraction;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;

/**
 * Responsibilities:
 * <ul>
 * <li>handle all proactive interactions like {@link AskKnowledgeInteraction}
 * and {@link PostKnowledgeInteraction}</li>
 * 
 * <li>determine the potential {@link KnowledgeBase}s for which matching
 * {@link KnowledgeInteraction}s should be found</li>
 * <li>reduce the potential {@link KnowledgeBase}s to the ones that actually
 * have compatible {@link KnowledgeInteraction}s</li>
 * <li>Send the {@link KnowledgeBase}s the correct messages via the message
 * dispatcher interface.</li>
 * <li>process the results from other {@link KnowledgeBase}s</li>
 * <li>contact the {@link KnowledgeBase} via the {@link SmartConnector}
 * interface</li>
 * <li>take into account that the reasoner will be added in a future
 * version</li>
 * <li></li>
 * </ul>
 */
public interface ProactiveInteractionProcessor {

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
	CompletableFuture<AskResult> processAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
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
	CompletableFuture<PostResult> processPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments);

}
