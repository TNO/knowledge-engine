package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class ReasonerPlan {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerPlan.class);

	private Map<BaseRule, ReasonerNode> ruleToReasonerNode;
	private BaseRule start;
	private ReasonerNode current;
	private Stack<ReasonerNode> stack;
	private RuleStore store;

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {

		this.ruleToReasonerNode = new HashMap<>();
		this.store = aStore;
		this.start = aStartRule;

		// build reasoning graph
		ReasonerNode startNode = createReasonerNode(aStartRule);

		// prepare execution
		this.stack = new Stack<>();
		stack.push(startNode);
	}

	public ReasonerNode getNode(BaseRule aRule) {
		return this.ruleToReasonerNode.get(aRule);
	}

	/**
	 * recursive building of the reasonernode graph
	 */
	private ReasonerNode createReasonerNode(BaseRule aRule) {

		ReasonerNode reasonerNode;
		if ((reasonerNode = getNode(aRule)) != null)
			return reasonerNode;

		// build the reasoner node graph
		reasonerNode = new ReasonerNode(aRule);
		this.ruleToReasonerNode.put(aRule, reasonerNode);

		if (isBackward()) {
			// for now we only are interested in antecedent neighbors.
			// TODO for looping we DO want to consider consequent neighbors as well.

			for (Map.Entry<BaseRule, Set<Match>> entry : this.store.getAntecedentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof Rule) {
					reasonerNode.addAntecedentNeighbor(createReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}
		} else {
			// interested in both consequent and antecedent neighbors

			for (Map.Entry<BaseRule, Set<Match>> entry : this.store.getConsequentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof Rule) {
					reasonerNode.addConsequentNeighbor(createReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}

			// antecedent neighbors are relevant to propagate bindings further
			for (Map.Entry<BaseRule, Set<Match>> entry : this.store.getAntecedentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof Rule) {
					reasonerNode.addAntecedentNeighbor(createReasonerNode(entry.getKey()), entry.getValue());
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
		// a tree).
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
		this.getNode(this.start)
				.setOutgoingAntecedentBindingSet(aBindingSet.toTripleVarBindingSet(this.start.getAntecedent()));
		int i = 0;
		boolean hasNextStep = false;
		while (!this.stack.isEmpty()) {
			System.out.println("Step " + ++i + ": " + this.stack.peek());
			hasNextStep = stepBackward();
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
		this.current = this.stack.peek();

		boolean hasNextStep = true;
		boolean removeCurrentFromStack = true;

		if (this.current.hasAntecedent()) {
			if (this.current.hasConsequent() && !this.current.hasOutgoingAntecedentBindingSet()) {
				BaseRule r = this.current.getRule();
				assert r instanceof Rule;
				Rule rr = (Rule) r;

				TransformBindingSetHandler bsh = rr.getInverseBindingSetHandler();

				BindingSet aBindingSet = bsh.handle(this.current.getIncomingConsequentBindingSet().toBindingSet())
						.get();
				this.current.setOutgoingAntecedentBindingSet(
						aBindingSet.toTripleVarBindingSet(this.current.getRule().getAntecedent()));
			}

			boolean allNeighborsHaveOutgoingConsequentBindingSets = true;
			for (Map.Entry<ReasonerNode, Set<Match>> neighborEntry : this.current.getAntecedentNeighbors().entrySet()) {
				ReasonerNode neighbor = neighborEntry.getKey();
				Set<Match> neighborMatch = neighborEntry.getValue();

				if (!neighbor.hasOutgoingConsequentBindingSet()) {
					TripleVarBindingSet neighborBS = this.current.getOutgoingAntecedentBindingSet()
							.translate(neighbor.getRule().getConsequent(), neighborMatch);

					if (!neighbor.hasIncomingConsequentBindingSet())
						neighbor.setIncomingConsequentBindingSet(neighborBS);
					this.stack.push(neighbor);
					allNeighborsHaveOutgoingConsequentBindingSets = false;
					removeCurrentFromStack = false;
				} else {
					// skip this neighbor
				}
			}

			if (allNeighborsHaveOutgoingConsequentBindingSets) {
				// create incoming antecedent bindingset
				TripleVarBindingSet combinedBindings = new TripleVarBindingSet(this.current.getRule().getAntecedent());
				for (Map.Entry<ReasonerNode, Set<Match>> neighborEntry : this.current.getAntecedentNeighbors()
						.entrySet()) {
					ReasonerNode neighbor = neighborEntry.getKey();
					Set<Match> neighborMatches = neighborEntry.getValue();
					assert neighbor.hasOutgoingConsequentBindingSet();
					TripleVarBindingSet neighborOutgoingConsequentBindingSet = neighbor
							.getOutgoingConsequentBindingSet();

					combinedBindings = combinedBindings.merge(neighborOutgoingConsequentBindingSet
							.translate(this.current.getRule().getAntecedent(), neighborMatches));
				}

				TripleVarBindingSet compatiblePatternBindingsOnly = combinedBindings
						.keepCompatible(this.current.getOutgoingAntecedentBindingSet());

				this.current.setIncomingAntecedentBindingSet(compatiblePatternBindingsOnly.getFullBindingSet());
			}

			if (this.current.hasConsequent() && this.current.hasIncomingAntecedentBindingSet()) {
				BaseRule r = this.current.getRule();
				assert r instanceof Rule;
				Rule rr = (Rule) r;
				TransformBindingSetHandler bsh = rr.getBindingSetHandler();
				BindingSet outgoingConsequentBindingSet = bsh
						.handle(this.current.getIncomingAntecedentBindingSet().toBindingSet()).get();
				this.current.setOutgoingConsequentBindingSet(
						outgoingConsequentBindingSet.toTripleVarBindingSet(this.current.getRule().getConsequent()));
			}

		} else {
			// just use the binding set handler to retrieve a binding set based on the given
			// binding set.
			BaseRule r = this.current.getRule();
			assert r instanceof Rule;
			Rule rr = (Rule) r;

			TransformBindingSetHandler bsh = rr.getBindingSetHandler();
			BindingSet aBindingSet = bsh.handle(this.current.getIncomingConsequentBindingSet().toBindingSet()).get();
			this.current.setOutgoingConsequentBindingSet(
					aBindingSet.toTripleVarBindingSet(this.current.getRule().getConsequent()));
		}

		if (removeCurrentFromStack) {
			this.stack.pop();
		}
		return hasNextStep;
	}

	public ReasonerNode getStartNode() {
		return this.getNode(this.start);
	}

	public boolean stepForward() throws InterruptedException, ExecutionException {
		boolean hasNextStep = true;
		boolean removeCurrentFromStack = true;

		if (this.current.hasConsequent()) {
			if (this.current.hasAntecedent() && !this.current.hasOutgoingConsequentBindingSet()) {
				BaseRule r = this.current.getRule();
				assert r instanceof Rule;
				Rule rr = (Rule) r;

				TransformBindingSetHandler bsh = rr.getBindingSetHandler();

				BindingSet aBindingSet = bsh.handle(this.current.getIncomingAntecedentBindingSet().toBindingSet())
						.get();
				this.current.setOutgoingConsequentBindingSet(
						aBindingSet.toTripleVarBindingSet(this.current.getRule().getAntecedent()));
			}

			for (Map.Entry<ReasonerNode, Set<Match>> neighborEntry : this.current.getAntecedentNeighbors().entrySet()) {
				ReasonerNode neighbor = neighborEntry.getKey();
				Set<Match> neighborMatch = neighborEntry.getValue();
				TripleVarBindingSet neighborBS = this.current.getOutgoingConsequentBindingSet()
						.translate(neighbor.getRule().getAntecedent(), neighborMatch);

				if (neighbor.hasIncomingAntecedentBindingSet())
					neighbor.setIncomingAntecedentBindingSet(neighborBS);

				this.stack.push(neighbor);
				removeCurrentFromStack = false;
			}

		} else {
			// just use the binding set handler to deal with the binding set.
			BaseRule r = this.current.getRule();
			assert r instanceof Rule;
			Rule rr = (Rule) r;

			SinkBindingSetHandler bsh = rr.getSinkBindingSetHandler();
			bsh.handle(this.current.getIncomingConsequentBindingSet().toBindingSet());
		}

		if (removeCurrentFromStack)
			this.stack.pop();

		return hasNextStep;
	}

}
