package eu.knowledge.engine.reasoner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class KeReasoner {

	private static final Logger LOG = LoggerFactory.getLogger(KeReasoner.class);

	// rules might need an order to prevent infinite loops
	private List<Rule> rules = new ArrayList<Rule>();

	public void addRule(Rule rule) {
		rules.add(rule);
	}

	public ReasoningNode backwardPlan(Set<TriplePattern> aGoal, MatchStrategy aMatchStrategy, TaskBoard aTaskboard) {
		Rule goalRule = new Rule(aGoal, new HashSet<>(), new BindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {

				CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();

				future.handle((r, e) -> {

					if (r == null) {
						LOG.error("An exception has occured", e);
						return null;
					} else {
						return r;
					}
				});
				future.complete(bs);
				return future;
			}

		});
		ReasoningNode root = new ReasoningNode(rules, null, goalRule, aMatchStrategy, true, aTaskboard);
		return root;
	}

	public ReasoningNode forwardPlan(Set<TriplePattern> aPremise, MatchStrategy aMatchStrategy, TaskBoard aTaskboard) {

		Rule premiseRule = new Rule(new HashSet<>(), aPremise, new BindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {
				CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();

				future.handle((r, e) -> {

					if (r == null) {
						LOG.error("An exception has occured", e);
						return null;
					} else {
						return r;
					}
				});
				future.complete(bs);
				return future;
			}
		});

		ReasoningNode root = new ReasoningNode(rules, null, premiseRule, aMatchStrategy, false, aTaskboard);

		return root;
	}

	public List<Rule> getRules() {
		return this.rules;
	}

}
