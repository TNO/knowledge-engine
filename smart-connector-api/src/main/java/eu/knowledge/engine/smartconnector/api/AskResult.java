package eu.knowledge.engine.smartconnector.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.api.TriplePattern;

/**
 * An {@link AskResult} contains the result of the
 * {@link AskKnowledgeInteraction}, of course including the {@link Binding}s,
 * but (in the future) also information on how the result is formed (which
 * {@link KnowledgeBase}s contributed etc.)
 */
public class AskResult {

	private static final Logger LOG = LoggerFactory.getLogger(AskResult.class);

	private final BindingSet bindings;

	/**
	 * Can be null, if the matcher is used instead of the reasoner.
	 */
	private ReasonerPlan reasonerPlan;
	
	private Set<Set<TriplePattern>> knowledgeGaps;

	private final Set<AskExchangeInfo> exchangeInfos;

	/**
	 * Create a {@link AskResult}.
	 * 
	 * @param someBindings A {@link BindingSet} that contains the solutions to an
	 *                     {@link AskKnowledgeInteraction} question. It is either
	 *                     empty, or contain one or more {@link Binding}s with a
	 *                     value for every available variable in the
	 *                     {@link GraphPattern}.
	 */
	public AskResult(BindingSet someBindings, Set<AskExchangeInfo> askExchangeInfos, ReasonerPlan aRootNode, Set<Set<TriplePattern>> kGaps) {
		this.bindings = someBindings;
		this.exchangeInfos = askExchangeInfos;
		this.reasonerPlan = aRootNode;
		this.knowledgeGaps = kGaps;
	}

	public AskResult(BindingSet someBindings, Set<AskExchangeInfo> askExchangeInfos) {
		this(someBindings, askExchangeInfos, null, null);
	}

	/**
	 * @return The {@link BindingSet} that contains the results of the
	 *         {@link AskKnowledgeInteraction} that happened.
	 */
	public BindingSet getBindings() {
		return this.bindings;
	}

	public Set<AskExchangeInfo> getExchangeInfoPerKnowledgeBase() {
		return Collections.unmodifiableSet(exchangeInfos);
	}

	public Duration getTotalExchangeTime() {

		Set<AskExchangeInfo> infos = this.getExchangeInfoPerKnowledgeBase();

		if (!infos.isEmpty()) {
			Instant start = Instant.MAX;
			Instant end = Instant.MIN;

			// find the earliest and latest instant among the exchange infos.
			for (AskExchangeInfo info : infos) {

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

	/**
	 * If the reasoner was enabled during this post interaction, this method returns
	 * detailed information about the steps taken during the reasoning process.
	 * Otherwise it returns {@code null}.
	 * 
	 * See {@link SmartConnector#setReasonerEnabled(boolean)}
	 * 
	 * @return
	 */
	public ReasonerPlan getReasonerPlan() {
		return this.reasonerPlan;
	}

	public Set<Set<TriplePattern>> getKnowledgeGaps() {
		return this.knowledgeGaps;
	}

	@Override
	public String toString() {
		return "AskResult [bindings=" + bindings + ", exchangeInfoPerKnowledgeBase=" + exchangeInfos + ", knowledgeGaps=" + knowledgeGaps + "]";
	}
}
