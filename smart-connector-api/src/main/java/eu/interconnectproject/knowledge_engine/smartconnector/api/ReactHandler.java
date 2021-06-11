package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link ReactHandler} provides a handler method
 * ({@link ReactHandler#react(ReactKnowledgeInteraction, BindingSet)}) that
 * returns a {@link BindingSet} for the provided input. Unlike in the
 * {@link AnswerHandler}, which only has a single {@link GraphPattern}, the
 * {@link GraphPattern} of the result *can* be different from the argument's.
 */
public interface ReactHandler {

	public default CompletableFuture<BindingSet> reactAsync(ReactKnowledgeInteraction anRKI, BindingSet argument) {
		CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
		BindingSet bs = this.react(anRKI, argument);
		future.complete(bs);
		return future;
	}

	/**
	 * Create a {@link ReactHandler}
	 *
	 * @param anRKI    The {@link KnowledgeInteraction} that is involved in this
	 *                 post/react process.
	 * @param argument The {@link BindingSet} for variables in the
	 *                 {@link KnowledgeInteraction}'s argument {@link GraphPattern}.
	 * @return A non-null but possibly empty {@link BindingSet} for variables in the
	 *         {@link KnowledgeInteraction}'s result {@link GraphPattern}.
	 */
	BindingSet react(ReactKnowledgeInteraction anRKI, BindingSet argument);
}
