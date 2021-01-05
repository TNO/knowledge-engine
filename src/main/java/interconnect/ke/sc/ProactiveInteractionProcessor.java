package interconnect.ke.sc;

import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.AskResult;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.PostResult;
import interconnect.ke.api.RecipientSelector;
import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.binding.BindingSet;
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

	CompletableFuture<AskResult> processAsk(AskKnowledgeInteraction anAKI, RecipientSelector aSelector,
			BindingSet aBindingSet);

	CompletableFuture<PostResult> processPost(PostKnowledgeInteraction aPKI, RecipientSelector aSelector,
			BindingSet someArguments);

}
