package eu.knowledge.engine.smartconnector.api;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ReactHandler} provides a handler method
 * ({@link ReactHandler#react(ReactKnowledgeInteraction, ReactExchangeInfo)})
 * that returns a {@link BindingSet} for the provided input. Unlike in the
 * {@link AnswerHandler}, which only has a single {@link GraphPattern}, the
 * {@link GraphPattern} of the result *can* be different from the argument's.
 */
public interface ReactHandler {

	/**
	 * Asynchronous handler for this interaction. The default implementation simply
	 * blocks and calls the synchronous method, but by overriding this default
	 * implementation, you can use an event-based architecture.
	 *
	 * @param anRKI              The {@link KnowledgeInteraction} that is involved
	 *                           in this post/react process.
	 * @param aReactExchangeInfo An {@link ReactExchangeInfo}, containing a
	 *                           {@link BindingSet} for variables in the
	 *                           {@link KnowledgeInteraction}'s argument
	 *                           {@link GraphPattern}, and more information such as
	 *                           the ID of the POSTing knowledge base.
	 * @return A future of a non-null but possibly empty {@link BindingSet} for
	 *         variables in the {@link KnowledgeInteraction}'s result
	 *         {@link GraphPattern}. Note that an empty binding set object must be
	 *         returned when the specific react knowledge interaction has no result
	 *         graph pattern. Should not return null.
	 */
	public default CompletableFuture<BindingSet> reactAsync(ReactKnowledgeInteraction anRKI,
			ReactExchangeInfo aReactExchangeInfo) {
		CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
		try {
			BindingSet bs = this.react(anRKI, aReactExchangeInfo);
			future.complete(bs);
		} catch (Exception e) {
			LoggerFactory.getLogger(ReactHandler.class)
					.error("Reacting should not result in the following exception.", e);
			future.completeExceptionally(e);

		}
		return future;
	}

	/**
	 * Create a {@link ReactHandler}
	 *
	 * @param anRKI              The {@link KnowledgeInteraction} that is involved
	 *                           in this post/react process.
	 * @param aReactExchangeInfo An {@link ReactExchangeInfo}, containing a
	 *                           {@link BindingSet} for variables in the
	 *                           {@link KnowledgeInteraction}'s argument
	 *                           {@link GraphPattern}, and more information such as
	 *                           the ID of the POSTing knowledge base.
	 * @return A non-null but possibly empty {@link BindingSet} for variables in the
	 *         {@link KnowledgeInteraction}'s result {@link GraphPattern}. Note that
	 *         an empty binding set object must be returned when the specific react
	 *         knowledge interaction has no result graph pattern. Should not return
	 *         null.
	 */
	BindingSet react(ReactKnowledgeInteraction anRKI, ReactExchangeInfo aReactExchangeInfo);
}
