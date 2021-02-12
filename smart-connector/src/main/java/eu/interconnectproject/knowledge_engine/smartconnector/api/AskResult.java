package eu.interconnectproject.knowledge_engine.smartconnector.api;

import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.AskKnowledgeInteraction;

/**
 * An {@link AskResult} contains the result of the
 * {@link AskKnowledgeInteraction}, of course including the {@link Binding}s,
 * but (in the future) also information on how the result is formed (which
 * {@link KnowledgeBase}s contributed etc.)
 */
public class AskResult {

	private final BindingSet bindings;

	/**
	 * Create a {@link AskResult}.
	 * 
	 * @param someBindings A {@link BindingSet} that contains the solutions to an
	 *                     {@link AskKnowledgeInteraction} question. It is either
	 *                     empty, or contain one or more {@link Binding}s with a
	 *                     value for every available variable in the
	 *                     {@link GraphPattern}.
	 */
	public AskResult(BindingSet someBindings) {
		this.bindings = someBindings;
	}

	/**
	 * @return The {@link BindingSet} that contains the results of the
	 *         {@link AskKnowledgeInteraction} that happened.
	 */
	public BindingSet getBindings() {
		return this.bindings;
	}
}
