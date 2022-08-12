package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class ReasonerPlan {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerPlan.class);

	private Map<BaseRule, RuleNode> ruleToReasonerNode;
	private ProactiveRule start;

	private LinkedList<RuleNode> stack;
	private RuleStore store;

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {

		this.ruleToReasonerNode = new HashMap<>();
		this.store = aStore;
		assert aStore.getRules().contains(aStartRule);
		this.start = aStartRule;

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
		// TODO prune is a bit different because we are working with a graph (instead of
		// a tree). Note though, that we cannot just look at the shortest path, because
		// some rules are special (i.e. have custom bindingsethandlers) and thus might
		// behave logically unexpected. So, we have to keep that in mind when
		// optimizing.
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

		boolean backward;
		if (this.start.getConsequent().isEmpty()) {
			this.getNode(this.start)
					.setOutgoingAntecedentBindingSet(aBindingSet.toTripleVarBindingSet(this.start.getAntecedent()));
			backward = true;
		} else {
			this.getNode(this.start)
					.setOutgoingConsequentBindingSet(aBindingSet.toTripleVarBindingSet(this.start.getConsequent()));
			backward = false;
		}

		int i = 0;
		boolean hasNextStep = false;
		while (!this.stack.isEmpty()) {
			System.out.println("Step " + ++i + ": " + this.stack.peek());
			if (backward)
				hasNextStep = stepBackward();
			else
				hasNextStep = stepForward();
		}
		return hasNextStep;
	}

	/**
	 * Takes a step in the reasoning process and returns {@code true} if another
	 * step can be taken, or {@code false} if no further step can be taken. This
	 * latter can be resolved by executing some of the task on the TaskBoard.
	 * 
	 * TODO rename to backward step and forward step (separate processes)
	 * 
	 * @return {@code true} when another step can be taken, {@code false} otherwise.
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public boolean stepBackward() throws InterruptedException, ExecutionException {
		RuleNode current = this.stack.peek();

		boolean hasNextStep = true;
		boolean removeCurrentFromStack = true;

		if (current.hasAntecedent()) {
			if (current.hasConsequent() && !current.hasOutgoingAntecedentBindingSet()) {
				current.applyBindingSetHandlerFromConsequentToAntecedent();
			}

			Set<RuleNode> neighbors = current.contactAntecedentNeighbors(current.getAntecedentNeighbors());

			this.stack.addAll(0, neighbors);

			if (neighbors.isEmpty()) {
				// create incoming antecedent bindingset
				current.collectIncomingAntecedentBindingSet();
			} else
				removeCurrentFromStack = false;

			if (current.hasConsequent() && current.hasIncomingAntecedentBindingSet()) {
				current.applyBindingSetHandlerFromAntecedentToConsequent();
			}

		} else {
			// just use the binding set handler to retrieve a binding set based on the given
			// binding set.
			current.applyBindingSetHandlerFromConsequentToConsequent();

		}

		if (removeCurrentFromStack) {
			this.stack.pop();
		}
		return hasNextStep;
	}

	public boolean stepForward() throws InterruptedException, ExecutionException {
		RuleNode current = this.stack.peek();
		boolean hasNextStep = true;
		boolean removeCurrentFromStack = true;
		RuleNode parent = null;
		if (this.stack.size() > 1)
			parent = this.stack.get(1);

		// TODO retrieve additional bindingsets via backward chaining?
		if (current.hasConsequent()) {
			if (current.hasAntecedent() && !current.hasOutgoingConsequentBindingSet()) {

				current.applyBindingSetHandlerFromAntecedentToConsequent();
			}

			Set<RuleNode> neighbors = current.contactConsequentNeighbors(current.getConsequentNeighbors());

			if (!neighbors.isEmpty())
				removeCurrentFromStack = false;

			this.stack.addAll(0, neighbors);

		} else {
			// just use the binding set handler to deal with the binding set.
			current.applyBindingSetHandlerToAntecedent();
		}

		if (removeCurrentFromStack)
			this.stack.pop();

		return hasNextStep;
	}

	public RuleNode getStartNode() {
		return this.getNode(this.start);
	}

}
