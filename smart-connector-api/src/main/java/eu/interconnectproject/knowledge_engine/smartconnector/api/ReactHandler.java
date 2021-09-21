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

	/**
	 * Asynchronous handler for this interaction. The default implementation
	 * simply blocks and calls the synchronous method, but by overriding this
	 * default implementation, you can use an event-based architecture.
	 *
	 * @param anRKI The {@link KnowledgeInteraction} that is involved in this
	 *              post/react process.
	 * @param aReactExchangeInfo An {@link ReactExchangeInfo}, containing a
	 *     {@link BindingSet} for variables in the {@link KnowledgeInteraction}'s
	 *     argument {@link GraphPattern}, and more information such as the ID of
	 *     the POSTing knowledge base.
	 * @return A future of a non-null but possibly empty {@link BindingSet} for
	 *         variables in the {@link KnowledgeInteraction}'s result
	 *         {@link GraphPattern}.
	 */
	public default CompletableFuture<BindingSet> reactAsync(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo) {
		CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
		BindingSet bs = this.react(anRKI, aReactExchangeInfo);
		future.complete(bs);
		return future;
	}

	/**
	 * Create a {@link ReactHandler}
	 *
	 * @param anRKI    The {@link KnowledgeInteraction} that is involved in this
	 *                 post/react process.
	 * @param aReactExchangeInfo
	 *   An {@link ReactExchangeInfo}, containing a {@link BindingSet} for
	 *   variables in the {@link KnowledgeInteraction}'s argument
	 *   {@link GraphPattern}, and more information such as the ID of the POSTing
	 *   knowledge base.
	 * @return A non-null but possibly empty {@link BindingSet} for variables in the
	 *         {@link KnowledgeInteraction}'s result {@link GraphPattern}.
	 */
	BindingSet react(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo);
}
