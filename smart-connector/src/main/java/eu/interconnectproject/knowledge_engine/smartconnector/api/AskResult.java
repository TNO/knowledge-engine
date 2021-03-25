package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AskResult} contains the result of the
 * {@link AskKnowledgeInteraction}, of course including the {@link Binding}s,
 * but (in the future) also information on how the result is formed (which
 * {@link KnowledgeBase}s contributed etc.)
 */
public class AskResult {

	private static final Logger LOG = LoggerFactory.getLogger(AskResult.class);

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

	public Duration getTotalExchangeTime() {

		Map<URI, AskExchangeInfo> infos = this.getExchangeInfoPerKnowledgeBase();

		if (!infos.isEmpty()) {
			Instant start = Instant.MAX;
			Instant end = Instant.MIN;

			// find the earliest and latest instant among the exchange infos.
			for (AskExchangeInfo info : infos.values()) {

				if (info.getExchangeStart().isBefore(start)) {
					start = info.getExchangeStart();
				}

				if (info.getExchangeEnd().isAfter(end)) {
					end = info.getExchangeEnd();
				}
			}
			return Duration.between(start, end);
		} else {
			return Duration.ofMillis(0);
		}

	}

	@Override
	public String toString() {
		return "AskResult [bindings=" + bindings + ", exchangeInfoPerKnowledgeBase=" + exchangeInfoPerKnowledgeBase
				+ "]";
	}
}
