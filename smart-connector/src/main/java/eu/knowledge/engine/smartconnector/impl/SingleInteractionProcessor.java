package eu.knowledge.engine.smartconnector.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.KnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.PostResult;

/**
 * This interface is used to abstract away the details of the matching/reasoning
 * process. Both the matcher and (in the future) the reasoner will implement
 * this interface in a different way which results in different behaviour of the
 * smart connector overall.
 *
 * It receives a {@link KnowledgeInteraction} with bindings and returns a set of
 * bindings. Depending on the type of {@link KnowledgeInteraction} these
 * bindings are for the single {@link GraphPattern} in the
 * {@link KnowledgeInteraction} or for the argument/result
 * {@link GraphPattern}s.
 */
public abstract class SingleInteractionProcessor {

	protected final Set<KnowledgeInteractionInfo> otherKnowledgeInteractions;
	protected final MessageRouter messageRouter;

	public SingleInteractionProcessor(Set<KnowledgeInteractionInfo> knowledgeInteractions,
			MessageRouter messageRouter) {
		this.otherKnowledgeInteractions = knowledgeInteractions;
		this.messageRouter = messageRouter;

	}

	/**
	 * Make a plan (if applicable) for executing the given Ask Knowledge
	 * Interaction.
	 * 
	 * @param aAKI
	 */
	abstract void planAskInteraction(MyKnowledgeInteractionInfo aAKI);

	/**
	 * Execute the plan (if applicable) and return an AskResult.
	 * 
	 * @param someBindings
	 * @return
	 */
	abstract CompletableFuture<AskResult> executeAskInteraction(BindingSet someBindings);

	/**
	 * Make a plan (if applicable) for executing the given PostKnowledgeInteraction
	 * 
	 * @param aPKI
	 */
	abstract void planPostInteraction(MyKnowledgeInteractionInfo aPKI);

	/**
	 * Execute the plan (if applicable) and return an PostResult.
	 * 
	 * @param someBindings
	 * @return
	 */
	abstract CompletableFuture<PostResult> executePostInteraction(BindingSet someBindings);

	// close?

}
