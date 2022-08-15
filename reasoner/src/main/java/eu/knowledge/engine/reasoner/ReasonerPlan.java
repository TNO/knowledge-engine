package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class ReasonerPlan {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerPlan.class);

	private Map<BaseRule, RuleNode> ruleToReasonerNode;
	private ProactiveRule start;

	/**
	 * Keeps track of the path through the graph and allows us to retrieve
	 * information about which nodes lie on our path to the startnode. This is
	 * helpful while reasoning backward while forward (to propagate data) and while
	 * reasoning forward while backward (in case of loops). The closer a node is to
	 * the start of the list, the most recent it was visited. TODO does this really
	 * need to be a linkedlist? Do we need to insert a lot of items in the middle
	 * and at the start? I think we can just use an arraylist for this.
	 */
	private LinkedList<RuleNode> stack;
	private RuleStore store;
	private Map<RuleNode, RuleNode> parentMap;

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {

		this.ruleToReasonerNode = new HashMap<>();
		this.store = aStore;
		assert aStore.getRules().contains(aStartRule);
		this.start = aStartRule;
		this.parentMap = new HashMap<>();

		// build reasoning graph
		RuleNode startNode = createOrGetReasonerNode(aStartRule);

		// prepare execution
		this.stack = new LinkedList<>();
		stack.push(startNode);
	}

	public RuleNode getNode(BaseRule aRule) {
		return this.ruleToReasonerNode.get(aRule);
	}

	/**
	 * recursive building of the reasonernode graph
	 */
	private RuleNode createOrGetReasonerNode(BaseRule aRule) {

		RuleNode reasonerNode;
		if ((reasonerNode = getNode(aRule)) != null)
			return reasonerNode;

		// build the reasoner node graph
		reasonerNode = new RuleNode(aRule);
		this.ruleToReasonerNode.put(aRule, reasonerNode);

		if (isBackward()) {
			// for now we only are interested in antecedent neighbors.
			// TODO for looping we DO want to consider consequent neighbors as well.

			for (Map.Entry<BaseRule, Set<Match>> entry : this.store.getAntecedentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof Rule) {
					reasonerNode.addAntecedentNeighbor(createOrGetReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}
		} else {
			// interested in both consequent and antecedent neighbors
			for (Map.Entry<BaseRule, Set<Match>> entry : this.store.getConsequentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof Rule) {
					reasonerNode.addConsequentNeighbor(createOrGetReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}

			// antecedent neighbors to propagate bindings further via backward chaining
			for (Map.Entry<BaseRule, Set<Match>> entry : this.store.getAntecedentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof Rule) {
					reasonerNode.addAntecedentNeighbor(createOrGetReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}

		}

		return reasonerNode;
	}

	public boolean isBackward() {

		return !this.start.getAntecedent().isEmpty() && this.start.getConsequent().isEmpty();
	}

	public void optimize() {
		/*
		 * TODO prune is a bit different because we are working with a graph (instead of
		 * a tree). Note though, that we cannot just look at the shortest path, because
		 * some rules are special (i.e. have custom bindingsethandlers) and thus might
		 * behave logically unexpected. So, we have to keep that in mind when
		 * optimizing.
		 */
	}

	/**
	 * Executes the plan until it can no longer proceed. If there are tasks on the
	 * TaskBoard after this method returns, make sure to execute the tasks and call
	 * this method again. If there are no more tasks on the TaskBoard after this
	 * method return, the reasoning process is finished.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public boolean execute(BindingSet aBindingSet) throws InterruptedException, ExecutionException {

		assert this.start.isProactive();

		if (this.start.getConsequent().isEmpty()) {
			this.getNode(this.start)
					.setOutgoingAntecedentBindingSet(aBindingSet.toTripleVarBindingSet(this.start.getAntecedent()));
		} else {
			this.getNode(this.start)
					.setOutgoingConsequentBindingSet(aBindingSet.toTripleVarBindingSet(this.start.getConsequent()));
		}

		int i = 0;
		boolean hasNextStep = false;
		while (!this.stack.isEmpty()) {
			System.out.println("Step " + ++i + ": " + this.stack.peek());
			step();
		}
		return hasNextStep;
	}

	/**
	 * The step logic consists of 3 parts:
	 * <ul>
	 * <li>from consequent to antecedent (backward only)</li>
	 * <li>push/pull bindingsets to/from antecedent neighbors</li>
	 * <li>from</li>
	 * </ul>
	 * Going backward and forward have an overlapping logic only it occurs either at
	 * the end of the step (in case of backward) or at the start of the step (in
	 * case of forward).
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void step() throws InterruptedException, ExecutionException {
		boolean removeCurrentFromStack = true;
		RuleNode current = this.stack.peek();

		RuleNode previous = null;
		if (this.stack.size() > 1)
			previous = this.parentMap.get(current);

		// determine our direction
		boolean forward = isForward(current, previous);

		if (!forward) {
			if (current.hasAntecedent() && current.hasConsequent()) {
				// from consequent to antecedent (only when backward)
				if (!current.hasOutgoingAntecedentBindingSet()) {
					assert current.hasIncomingConsequentBindingSet();
					current.applyBindingSetHandlerFromConsequentToAntecedent();
					assert current.hasOutgoingAntecedentBindingSet();
				}
			} else if (!current.hasAntecedent() && current.hasConsequent()) {
				// just use the binding set handler to retrieve a binding set based on the given
				// binding set.
				assert current.hasConsequent();
				assert current.hasIncomingConsequentBindingSet();
				assert !forward;
				if (!current.hasOutgoingConsequentBindingSet()) {
					current.applyBindingSetHandlerFromConsequentToConsequent();
				}
			} else if (current.hasAntecedent() && !current.hasConsequent()) {
				assert current.equals(this.getNode(this.start));
			} else
				assert false;
		}

		if (current.hasAntecedent()) {

			if (!current.hasIncomingAntecedentBindingSet()) {
				// push/pull bindingsets to/from antecedents (always)
				TripleVarBindingSet aBindingSet;
				if (forward) {
					aBindingSet = previous.getOutgoingConsequentBindingSet();

					Set<Match> previousMatches = Rule.matches(previous.getRule().getConsequent(),
							current.getRule().getAntecedent(), MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);

//					Set<Match> previousMatches = current.getAntecedentNeighbors().get(previous);
					aBindingSet = aBindingSet.translate(current.getRule().getAntecedent(), previousMatches);
				} else {
					aBindingSet = current.getOutgoingAntecedentBindingSet();
				}
				Set<RuleNode> antecedentNeighbors = current.contactAntecedentNeighbors2(aBindingSet);

				for (RuleNode rn : antecedentNeighbors) {
					this.stack.push(rn);
					this.parentMap.put(rn, current);
				}

				if (!antecedentNeighbors.isEmpty())
					removeCurrentFromStack = false;
				else {
					// create incoming antecedent bindingset
					if (forward) {
						aBindingSet = previous.getOutgoingConsequentBindingSet();

						Set<Match> previousMatches = Rule.matches(previous.getRule().getConsequent(),
								current.getRule().getAntecedent(), MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);

//						Set<Match> previousMatches = current.getAntecedentNeighbors().get(previous);
						aBindingSet = aBindingSet.translate(current.getRule().getAntecedent(), previousMatches);
					} else {
						aBindingSet = current.getOutgoingAntecedentBindingSet();
					}

					current.collectIncomingAntecedentBindingSet(aBindingSet);
				}
			}

			// from antecedent to consequent (always)
			if (current.hasConsequent()) {
				if (current.hasIncomingAntecedentBindingSet() && !current.hasOutgoingConsequentBindingSet()) {
					current.applyBindingSetHandlerFromAntecedentToConsequent();
				}
			} else {
				// just use the binding set handler to deal with the binding set.
				// TODO see if we can remove this {@code forward} variable from this if.
				if (forward && current.hasIncomingAntecedentBindingSet()) {
					current.applyBindingSetHandlerToAntecedent();
				}
			}
		}

		if (forward) {
			if (current.hasConsequent()) {
				// activate consequent neighbors
				assert current.hasOutgoingConsequentBindingSet();
				// if we activate neighbors, we do not remove current
				Set<RuleNode> neighbors = current.contactConsequentNeighbors(current.getConsequentNeighbors());

				for (RuleNode rn : neighbors) {
					this.stack.push(rn);
					this.parentMap.put(rn, current);
				}

				if (!neighbors.isEmpty())
					removeCurrentFromStack = false;
			}
		}

		if (removeCurrentFromStack)
			this.stack.pop();
	}

	public boolean isForward(RuleNode current, RuleNode previous) {
		boolean toConsequent = true;
		if (current.hasAntecedent() && current.hasConsequent()) {
			// determine based on previous node
			if (current.getConsequentNeighbors().containsKey(previous)) {
				toConsequent = false;
			}

		} else if (current.hasAntecedent() && !current.hasConsequent()) {
			if (current.equals(this.getNode(this.start))) {
				toConsequent = false;
			}
		} else if (!current.hasAntecedent() && current.hasConsequent()) {
			if (!current.equals(this.getNode(this.start))) {
				toConsequent = false;
			}
		} else
			assert false;
		return toConsequent;
	}

	public RuleNode getStartNode() {
		return this.getNode(this.start);
	}

}
