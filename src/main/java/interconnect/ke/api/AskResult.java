package interconnect.ke.api;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;

/**
 * An {@link AskResult} contains the result of the
 * {@link AskKnowledgeInteraction}, of course including the {@link Binding}s,
 * but (in the future) also information on how the result is formed (which
 * {@link KnowledgeBase}s contributed etc.)
 */
public class AskResult {

	private final BindingSet bindings;
	private final Map<URI, AskExchangeInfo> exchangeInfoPerKnowledgeBase;

	/**
	 * Create a {@link AskResult}.
	 * 
	 * @param someBindings A {@link BindingSet} that contains the solutions to an
	 *                     {@link AskKnowledgeInteraction} question. It is either
	 *                     empty, or contain one or more {@link Binding}s with a
	 *                     value for every available variable in the
	 *                     {@link GraphPattern}.
	 */
	public AskResult(BindingSet someBindings, Set<ExchangeInfo> askExchangeInfos) {
		this.bindings = someBindings;

		exchangeInfoPerKnowledgeBase = askExchangeInfos.stream()
				.collect(Collectors.toMap(x -> x.getKnowledgeBaseId(), x -> (AskExchangeInfo) x));

	}

	/**
	 * @return The {@link BindingSet} that contains the results of the
	 *         {@link AskKnowledgeInteraction} that happened.
	 */
	public BindingSet getBindings() {
		return this.bindings;
	}

	public Map<URI, AskExchangeInfo> getExchangeInfoPerKnowledgeBase() {
		return Collections.unmodifiableMap(exchangeInfoPerKnowledgeBase);
	}

}
