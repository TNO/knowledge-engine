package eu.interconnectproject.knowledge_engine.smartconnector.api;

import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeInteraction;

/**
 * An {@link AnswerHandler} provides a handler
 * {@code AnswerHandler#answer(AnswerKnowledgeInteraction, BindingSet)} method
 * that returns a {@link BindingSet} for the provided input.
 */
public interface AnswerHandler {
	/**
	 * @param anAKI       The {@link KnowledgeInteraction} that is involved in the
	 *                    question top answer.
	 * @param aBindingSet A set of {@link Binding}s for variables in the
	 *                    {@link KnowledgeInteraction}'s {@link GraphPattern}.
	 * @return All {@link Binding}s that match this {@link KnowledgeInteraction}'s
	 *         {@link GraphPattern}, AND matches ANY of the input BindingSet. All
	 *         variables from the {@link GraphPattern} are bound, including the ones
	 *         that were already provided in the input {@link BindingSet}. If not,
	 *         you would not be able to know which partial binding output
	 *         corresponded with which partial binding input.
	 */
	public BindingSet answer(AnswerKnowledgeInteraction anAKI, BindingSet aBindingSet);
}
