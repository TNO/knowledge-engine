package eu.knowledge.engine.smartconnector.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ReasonerPlan;

/**
 * A {@link PostResult} contains the result of the
 * {@link PostKnowledgeInteraction}, of course including the {@link Binding}s,
 * but (in the future) also information on how the result is formed (which
 * {@link KnowledgeBase}s contributed etc.)
 */
public class PostResult {

	private static final Logger LOG = LoggerFactory.getLogger(PostResult.class);

	private final BindingSet bindings;
	private final Set<PostExchangeInfo> exchangeInfos;
	/**
	 * Can be null if the matcher is used instead of the reasoner.
	 */
	private final ReasonerPlan rootNode;

	/**
	 * Create a {@link PostResult}.
	 * 
	 * @param someBindings A {@link BindingSet} that contains the solutions to an
	 *                     {@link PostKnowledgeInteraction} question. It is either
	 *                     empty, or contains one or more {@link Binding}s with a
	 *                     value for every available variable in the
	 *                     {@link GraphPattern}.
	 */
	public PostResult(BindingSet someBindings, Set<PostExchangeInfo> postExchangeInfos) {
		this(someBindings, postExchangeInfos, null);
	}

	public PostResult(BindingSet someBindings, Set<PostExchangeInfo> postExchangeInfos, ReasonerPlan aNode) {
		this.bindings = someBindings;
		this.exchangeInfos = postExchangeInfos;
		this.rootNode = aNode;
	}

	/**
	 * @return The {@link BindingSet} that contains the results of the
	 *         {@link PostKnowledgeInteraction} that happened.
	 */
	public BindingSet getBindings() {
		return this.bindings;
	}

	public Set<PostExchangeInfo> getExchangeInfoPerKnowledgeBase() {
		return Collections.unmodifiableSet(exchangeInfos);
	}

	public Duration getTotalExchangeTime() {

		Set<PostExchangeInfo> infos = this.getExchangeInfoPerKnowledgeBase();

		if (!infos.isEmpty()) {
			Instant start = Instant.MAX;
			Instant end = Instant.MIN;

			// find the earliest and latest instant among the exchange infos.
			for (PostExchangeInfo info : infos) {

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
		return this.rootNode;
	}

	@Override
	public String toString() {
		return "PostResult [bindings=" + bindings + ", exchangeInfoPerKnowledgeBase=" + exchangeInfos + "]";
	}
}
