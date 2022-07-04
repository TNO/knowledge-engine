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

public class ReasonerPlan {

	private static final Logger LOG = LoggerFactory.getLogger(ReasonerPlan.class);

	private Map<Rule, ReasonerNode> ruleToReasonerNode;
	private Rule start;
	private ReasonerNode current;
	private Stack<ReasonerNode> stack;
	private RuleStore store;

	public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {

		this.ruleToReasonerNode = new HashMap<>();
		this.store = aStore;
		this.start = aStartRule;

		// build reasoning graph
		createReasonerNode(aStartRule);

		// prepare execution
		this.stack = new Stack<>();
		stack.push(this.getNode(this.start));
	}

	public ReasonerNode getNode(Rule aRule) {
		return this.ruleToReasonerNode.get(aRule);
	}

	/**
	 * recursive building of the reasonernode graph
	 */
	private ReasonerNode createReasonerNode(Rule aRule) {

		ReasonerNode reasonerNode;
		if ((reasonerNode = getNode(aRule)) == null)
			return reasonerNode;

		// build the reasoner node graph
		// we make sure only reasonernodes are added as neighbors if they are relevant
		// to the execution. So, during execution we do not need to consider again
		// whether to visit a particular neighbor.
		reasonerNode = new ReasonerNode(aRule);
		this.ruleToReasonerNode.put(aRule, reasonerNode);

		// do we need to consider only antecedent neighbors, only consequent neighbors
		// or both? First look at whether we are forward or backward chaining (this
		// depends on the structure of the start rule).

		if (isBackward()) {
			// for now we only are interested in antecedent neighbors.
			// TODO for looping we DO want to consider consequent neighbors as well.

			for (Map.Entry<Rule, Set<Match>> entry : this.store.getAntecedentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof ReactiveRule) {
					reasonerNode.addAntecedentNeighbor(createReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}
		} else {
			// interested in both consequent and antecedent neighbors

			for (Map.Entry<Rule, Set<Match>> entry : this.store.getConsequentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof ReactiveRule) {
					reasonerNode.addConsequentNeighbor(createReasonerNode(entry.getKey()), entry.getValue());
				} else {
					LOG.trace("Skipped proactive rule: {}", entry.getKey());
				}
			}

			// antecedent neighbors are relevant to propagate bindings further
			for (Map.Entry<Rule, Set<Match>> entry : this.store.getAntecedentNeighbors(aRule).entrySet()) {

				if (entry.getKey() instanceof ReactiveRule) {
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
	public void execute(BindingSet aBindingSet) throws InterruptedException, ExecutionException {
		boolean hasNext = true;

		this.getNode(this.start)
				.setOutgoingAntecedentBindingSet(aBindingSet.toGraphBindingSet(this.start.getAntecedent()));

		while (hasNext) {
			hasNext = stepBackward();
		}
	}

	/**
	 * Takes a step in the reasoning process and returns {@code true} if another
	 * step can be taken, or {@code false} if no further step can be taken. This
	 * latter can be resolved by executing some of the task on the TaskBoard.
	 * 
	 * each of the following steps are optional, but not all possibilities are
	 * valid.
	 * <table border="1">
	 * <tr>
	 * <th>antecedent</th>
	 * <th>apply</th>
	 * <th>consequent</th>
	 * <th>remark</th>
	 * </tr>
	 * <tr>
	 * <td>no</td>
	 * <td>no</td>
	 * <td>no</td>
	 * <td>not possible</td>
	 * </tr>
	 * <tr>
	 * <td>yes</td>
	 * <td>no</td>
	 * <td>no</td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>yes</td>
	 * <td>yes</td>
	 * <td>no</td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>yes</td>
	 * <td>no</td>
	 * <td>yes</td>
	 * <td>not possible</td>
	 * </tr>
	 * <tr>
	 * <td>yes</td>
	 * <td>yes</td>
	 * <td>yes</td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>no</td>
	 * <td>yes</td>
	 * <td>no</td>
	 * <td>not possible</td>
	 * </tr>
	 * <tr>
	 * <td>no</td>
	 * <td>no</td>
	 * <td>yes</td>
	 * <td></td>
	 * </tr>
	 * <tr>
	 * <td>no</td>
	 * <td>yes</td>
	 * <td>yes</td>
	 * <td></td>
	 * </tr>
	 * </table>
	 * 
	 * TODO rename to backward step and forward step (separate processes)
	 * 
	 * @return {@code true} when another step can be taken, {@code false} otherwise.
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public boolean stepBackward() throws InterruptedException, ExecutionException {
		boolean canStepFurther = false;
		this.current = this.stack.peek();

		// check if we should propagate BindingSets or handle BindingSets
		// (readyForPropagate):
		// - if there is a outgoing antecedent binding set or a outgoing consequent binding set, we propagate
		// - if there is a incoming consequent binding set, we handle

		TripleVarBindingSet incomingAntecedentBS;

		if (this.current.hasAntecedent() && !this.current.hasConsequent()) {

			if ((incomingAntecedentBS = this.current.getIncomingAntecedentBindingSet()) != null) {
				antecedentOnlyIncomingAntecedentBindingSetAvailable();
			} else {
				antecedentOnlyIncomingAntecedentBindingSetNotAvailable();
			}

		} else if (this.current.hasAntecedent() && this.current.hasConsequent()) {

			if ((incomingAntecedentBS = this.current.getIncomingAntecedentBindingSet()) != null) {
				antecedentAndConsequentIncomingAntecedentBindingSetAvailable();
			} else {
				antecedentAndConsequentIncomingAntecedentBindingSetNotAvailable();
			}
		} else if (!this.current.hasAntecedent() && this.current.hasConsequent()) {
			if ((incomingAntecedentBS = this.current.getIncomingAntecedentBindingSet()) != null) {
				consequentOnlyIncomingAntecedentBindingSetAvailable();
			} else {
				consequentOnlyIncomingAntecedentBindingSetNotAvailable();
			}
		} else {
			throw new IllegalStateException(
					"A rule should have either an antecedent or a consequent or both: " + this.current.getRule());
		}

		// TODO collect an incoming antecedent bindingset

		// TODO apply this rule

		// TODO send an outgoing consequent bindingset

		return canStepFurther;
	}

	private boolean readyToPropagate(ReasonerNode aNode) {
		return aNode.getOutgoingAntecedentBindingSet() != null || aNode.getOutgoingConsequentBindingSet() != null;
	}

	private void consequentOnlyIncomingAntecedentBindingSetNotAvailable() {
		// TODO Auto-generated method stub

	}

	private void consequentOnlyIncomingAntecedentBindingSetAvailable() {
		// TODO Auto-generated method stub

	}

	private void antecedentAndConsequentIncomingAntecedentBindingSetNotAvailable() {
		// TODO Auto-generated method stub

	}

	private void antecedentAndConsequentIncomingAntecedentBindingSetAvailable()
			throws InterruptedException, ExecutionException {
		TripleVarBindingSet incomingAntecedentBS, outgoingConsequentBS;
		// we are ready to rumble
//		BindingSetHandler bsh = this.current.getRule().getForwardBindingSetHandler();
		// TODO use the taskboard if available
//		BindingSet tempBS = bsh.handle(incomingAntecedentBS.toBindingSet()).get();
//		outgoingConsequentBS = new TripleVarBindingSet(this.current.getRule().getConsequent(), tempBS);
//		this.current.setOutgoingConsequentBindingSet(outgoingConsequentBS);
	}

	private void antecedentOnlyIncomingAntecedentBindingSetAvailable() {
		// TODO Auto-generated method stub

	}

	private void antecedentOnlyIncomingAntecedentBindingSetNotAvailable() {
		TripleVarBindingSet incomingAntecedentBS, outgoingConsequentBS;

		// collect all antecedent neighbor's outgoing consequent bindings
		for (Map.Entry<ReasonerNode, Set<Match>> neighborEntry : this.current.getAntecedentNeighbors().entrySet()) {

			ReasonerNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatch = neighborEntry.getValue();

			if (neighbor.getOutgoingConsequentBindingSet() != null) {

			} else {

			}

//			TripleVarBindingSet neighborBS = bs.translate(neighbor.getRule().getAntecedent(),
//					neighborMatch);
//			neighbor.setIncomingConsequentBindingSet(neighborBS);
//			this.stack.push(neighbor);
		}

		// translate them to our incoming antecedent bindingset

//		outgoingConsequentBS = new TripleVarBindingSet(this.current.getRule().getConsequent(), tempBS);
//		this.current.setOutgoingConsequentBindingSet(outgoingConsequentBS);

		if ((outgoingConsequentBS = this.current.getOutgoingConsequentBindingSet()) != null) {
			TripleVarBindingSet bs = this.current.getOutgoingAntecedentBindingSet();
			for (Map.Entry<ReasonerNode, Set<Match>> neighborEntry : this.current.getAntecedentNeighbors().entrySet()) {
				ReasonerNode neighbor = neighborEntry.getKey();
				Set<Match> neighborMatch = neighborEntry.getValue();
				TripleVarBindingSet neighborBS = bs.translate(neighbor.getRule().getAntecedent(), neighborMatch);
				neighbor.setIncomingConsequentBindingSet(neighborBS);
				this.stack.push(neighbor);
			}
		}
	}

	public boolean stepForward() {
		boolean canStepFurther = false;

		return canStepFurther;
	}

}
