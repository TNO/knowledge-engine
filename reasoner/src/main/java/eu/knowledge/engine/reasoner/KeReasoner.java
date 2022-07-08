package eu.knowledge.engine.reasoner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class KeReasoner {

	// rules might need an order to prevent infinite loops
	private RuleStore store = new RuleStore();

	public void addRule(BaseRule rule) {
		store.addRule(rule);
	}

	public ReasoningNode backwardPlan(Set<TriplePattern> aGoal, MatchStrategy aMatchStrategy, TaskBoard aTaskboard) {
		Rule goalRule = new Rule(aGoal, new HashSet<>(), new TransformBindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {

				CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
				future.complete(bs);
				return future;
			}

		});
		this.store.addRule(goalRule);

		Set<Rule> rules = new HashSet<>();

		for (BaseRule r : this.store.getRules()) {
			assert r instanceof Rule;
			rules.add((Rule) r);
		}

		ReasoningNode root = new ReasoningNode(new ArrayList<>(rules), null, goalRule, aMatchStrategy, true,
				aTaskboard);
		return root;
	}

	public ReasoningNode forwardPlan(Set<TriplePattern> aPremise, MatchStrategy aMatchStrategy, TaskBoard aTaskboard) {
		Rule premiseRule = new Rule(new HashSet<>(), aPremise, new TransformBindingSetHandler() {

			/**
			 * The root node should just return the bindingset as is.
			 */
			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {
				CompletableFuture<BindingSet> future = new CompletableFuture<BindingSet>();
				future.complete(bs);
				return future;
			}
		});

		Set<Rule> rules = new HashSet<>();

		for (BaseRule r : this.store.getRules()) {
			assert r instanceof Rule;
			rules.add((Rule) r);
		}

		ReasoningNode root = new ReasoningNode(new ArrayList<>(rules), null, premiseRule, aMatchStrategy, false,
				aTaskboard);

		return root;
	}

	public List<BaseRule> getRules() {
		return new ArrayList<>(this.store.getRules());
	}

}
