package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.util.concurrent.CompletableFuture;

/**
 * An {@link AnswerHandler} provides a handler
 * {@code AnswerHandler#answer(AnswerKnowledgeInteraction, BindingSet)} method
 * that returns a {@link BindingSet} for the provided input.
 */
public interface AnswerHandler {

	/**
	 * Asynchronous handler for this interaction. The default implementation
	 * simply blocks and calls the synchronous method, but by overriding this
	 * default implementation, you can use an event-based architecture.
	 * 
	 * @param anAKI The {@link KnowledgeInteraction} that is involved in the
	 *              question top answer.
	 * @param anAnswerExchangeInfo An {@link AnswerExchangeInfo}, containing a set
	 *   of {@link Binding}s for variables in the {@link KnowledgeInteraction}'s
	 *   {@link GraphPattern}, and more information such as the ID of the ASKing
	 *   knowledge base.
	 * @return A future of all {@link Binding}s that match this
	 *   {@link KnowledgeInteraction}'s {@link GraphPattern}, AND matches ANY of
	 *   the input BindingSet. All variables from the {@link GraphPattern} are
	 *   bound, including the ones that were already provided in the input
	 *   {@link BindingSet}. If not, you would not be able to know which partial
	 *   binding output corresponded with which partial binding input.
	 */
	public default CompletableFuture<BindingSet> answerAsync(AnswerKnowledgeInteraction anAKI, AnswerExchangeInfo anAnswerExchangeInfo) {
		CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
		BindingSet bs = this.answer(anAKI, anAnswerExchangeInfo);
		future.complete(bs);
		return future;
	}

	/**
	 * @param anAKI       The {@link KnowledgeInteraction} that is involved in the
	 *                    question top answer.
	 * @param anAnswerExchangeInfo
	 *   An {@link AnswerExchangeInfo}, containing a set of {@link Binding}s for
	 *   variables in the {@link KnowledgeInteraction}'s {@link GraphPattern}, and
	 *   more information such as the ID of the ASKing knowledge base.
	 * @return All {@link Binding}s that match this {@link KnowledgeInteraction}'s
	 *         {@link GraphPattern}, AND matches ANY of the input BindingSet. All
	 *         variables from the {@link GraphPattern} are bound, including the ones
	 *         that were already provided in the input {@link BindingSet}. If not,
	 *         you would not be able to know which partial binding output
	 *         corresponded with which partial binding input.
	 */
	public abstract BindingSet answer(AnswerKnowledgeInteraction anAKI, AnswerExchangeInfo anAnswerExchangeInfo);
}
