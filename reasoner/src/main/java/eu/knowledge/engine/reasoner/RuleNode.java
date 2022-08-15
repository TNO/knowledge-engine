package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class RuleNode {

	/**
	 * The rule node from which this reasoner node has been derived.
	 */
	private BaseRule rule;

	/**
	 * All possible bindingsets. Not all of them are used in every situation.
	 */
	private TripleVarBindingSet incomingAntecedentBindingSet;
	private TripleVarBindingSet outgoingAntecedentBindingSet;
	private TripleVarBindingSet incomingConsequentBindingSet;
	private TripleVarBindingSet outgoingConsequentBindingSet;

	/**
	 * All relevant rules from the {@link RuleStore} whose consequents match this
	 * rule's antecedent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> antecedentNeighbors;

	/**
	 * All relevant rules from the {@link RuleStore} whose antecedents match this
	 * rule's consequent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> consequentNeighbors;

	/**
	 * Create a new ReasonerNode for the rule of the given {@code aRule}.
	 * 
	 * @param aRule The rule of which to create a reasoner node.
	 */
	public RuleNode(BaseRule aRule) {
		this.antecedentNeighbors = new HashMap<>();
		this.consequentNeighbors = new HashMap<>();
		this.rule = aRule;
	}

	public BaseRule getRule() {
		return rule;
	}

	public TripleVarBindingSet getIncomingAntecedentBindingSet() {
		assert !this.getRule().getAntecedent().isEmpty();
		return incomingAntecedentBindingSet;
	}

	public boolean hasIncomingAntecedentBindingSet() {
		return incomingAntecedentBindingSet != null;
	}

	public void setIncomingAntecedentBindingSet(TripleVarBindingSet incomingAntecedentBindingSet) {
		assert !this.getRule().getAntecedent().isEmpty();
		this.incomingAntecedentBindingSet = incomingAntecedentBindingSet;
	}

	public TripleVarBindingSet getOutgoingAntecedentBindingSet() {
		assert !this.getRule().getAntecedent().isEmpty();
		return outgoingAntecedentBindingSet;
	}

	public boolean hasOutgoingAntecedentBindingSet() {
		return outgoingAntecedentBindingSet != null;
	}

	public void setOutgoingAntecedentBindingSet(TripleVarBindingSet outgoingAntecedentBindingSet) {
		assert !this.getRule().getAntecedent().isEmpty();
		this.outgoingAntecedentBindingSet = outgoingAntecedentBindingSet;
	}

	public TripleVarBindingSet getIncomingConsequentBindingSet() {
		assert !this.getRule().getConsequent().isEmpty();
		return incomingConsequentBindingSet;
	}

	public boolean hasIncomingConsequentBindingSet() {
		return incomingConsequentBindingSet != null;
	}

	public void setIncomingConsequentBindingSet(TripleVarBindingSet incomingConsequentBindingSet) {
		assert !this.getRule().getConsequent().isEmpty();
		this.incomingConsequentBindingSet = incomingConsequentBindingSet;
	}

	public TripleVarBindingSet getOutgoingConsequentBindingSet() {
		assert !this.getRule().getConsequent().isEmpty();
		return outgoingConsequentBindingSet;
	}

	public boolean hasOutgoingConsequentBindingSet() {
		return outgoingConsequentBindingSet != null;
	}

	public void setOutgoingConsequentBindingSet(TripleVarBindingSet outgoingConsequentBindingSet) {
		assert !this.getRule().getConsequent().isEmpty();
		this.outgoingConsequentBindingSet = outgoingConsequentBindingSet;
	}

	public Map<RuleNode, Set<Match>> getAntecedentNeighbors() {
		assert !this.getRule().getAntecedent().isEmpty();
		return antecedentNeighbors;
	}

	public void addAntecedentNeighbor(RuleNode aReasonerNode, Set<Match> someMatches) {
		if (this.antecedentNeighbors.put(aReasonerNode, someMatches) == null) {
			aReasonerNode.addConsequentNeighbor(this, Match.invertAll(someMatches));
		}
	}

	public void addConsequentNeighbor(RuleNode aReasonerNode, Set<Match> someMatches) {
		if (this.consequentNeighbors.put(aReasonerNode, someMatches) == null) {
			aReasonerNode.addAntecedentNeighbor(this, Match.invertAll(someMatches));
		}

	}

	public Map<RuleNode, Set<Match>> getConsequentNeighbors() {
		assert !this.getRule().getConsequent().isEmpty();
		return consequentNeighbors;
	}

	public void applyBindingSetHandlerFromConsequentToAntecedent() throws InterruptedException, ExecutionException {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;

		TransformBindingSetHandler bsh = rr.getInverseBindingSetHandler();

		BindingSet aBindingSet = bsh.handle(this.getIncomingConsequentBindingSet().toBindingSet()).get();
		this.setOutgoingAntecedentBindingSet(aBindingSet.toTripleVarBindingSet(this.getRule().getAntecedent()));
	}

	public void applyBindingSetHandlerFromAntecedentToConsequent() throws InterruptedException, ExecutionException {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;
		TransformBindingSetHandler bsh = rr.getBindingSetHandler();

		TripleVarBindingSet aBindingSet = this.getIncomingAntecedentBindingSet();

		BindingSet outgoingConsequentBindingSet = bsh.handle(aBindingSet.toBindingSet()).get();
		this.setOutgoingConsequentBindingSet(
				outgoingConsequentBindingSet.toTripleVarBindingSet(this.getRule().getConsequent()));
	}

	public Set<RuleNode> contactConsequentNeighbors(Map<RuleNode, Set<Match>> someConsequentNeighbors) {
		Set<RuleNode> neighbors = new HashSet<>();
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : someConsequentNeighbors.entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatches = neighborEntry.getValue();
			TripleVarBindingSet neighborBS = this.getOutgoingConsequentBindingSet()
					.translate(neighbor.getRule().getAntecedent(), Match.invertAll(neighborMatches));

			// TODO do we want the merge to happen inside the translate method (in some
			// cases)?
			neighborBS = neighborBS.merge(neighborBS);

			if (!neighbor.hasIncomingAntecedentBindingSet()) {
//				neighbor.setIncomingAntecedentBindingSet(neighborBS);
				neighbors.add(neighbor);
			}

		}
		return neighbors;
	}

	public Set<RuleNode> contactAntecedentNeighbors(TripleVarBindingSet aBindingSet) {

		Set<RuleNode> neighbors = new HashSet<>();
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getAntecedentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatch = neighborEntry.getValue();

			if (!neighbor.hasOutgoingConsequentBindingSet()) {
				TripleVarBindingSet neighborBS = aBindingSet.translate(neighbor.getRule().getConsequent(),
						Match.invertAll(neighborMatch));

				if (!neighbor.hasIncomingConsequentBindingSet())
					neighbor.setIncomingConsequentBindingSet(neighborBS);
				neighbors.add(neighbor);
			} else {
				// skip this neighbor
			}
		}
		return neighbors;
	}

	public void applyBindingSetHandlerFromConsequentToConsequent() throws InterruptedException, ExecutionException {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;

		TransformBindingSetHandler bsh = rr.getBindingSetHandler();
		BindingSet aBindingSet = bsh.handle(this.getIncomingConsequentBindingSet().toBindingSet()).get();
		this.setOutgoingConsequentBindingSet(aBindingSet.toTripleVarBindingSet(this.getRule().getConsequent()));
	}

	public void applyBindingSetHandlerToAntecedent() {
		BaseRule r = this.getRule();
		assert r instanceof Rule;
		Rule rr = (Rule) r;

		TripleVarBindingSet aBindingSet = this.getIncomingAntecedentBindingSet();
		if (!isEmpty(aBindingSet)) {
			SinkBindingSetHandler bsh = rr.getSinkBindingSetHandler();
			bsh.handle(aBindingSet.toBindingSet());
		}
	}

	/**
	 * Collect the bindingsets from antecedent neighbors and make sure they are
	 * compatible with the incoming bindingset. This is either the outgoing
	 * antecedent bindingset (in case of backward chaining) or the outgoing
	 * consequent bindingset of our parent neighbor (in case of backward while
	 * forward chaining)
	 * 
	 * @param aBindingSet
	 */
	public void collectIncomingAntecedentBindingSet(TripleVarBindingSet aBindingSet) {

		TripleVarBindingSet combinedBindings = new TripleVarBindingSet(this.getRule().getAntecedent());
		for (Map.Entry<RuleNode, Set<Match>> neighborEntry : this.getAntecedentNeighbors().entrySet()) {
			RuleNode neighbor = neighborEntry.getKey();
			Set<Match> neighborMatches = neighborEntry.getValue();
			assert neighbor.hasOutgoingConsequentBindingSet();
			TripleVarBindingSet neighborOutgoingConsequentBindingSet = neighbor.getOutgoingConsequentBindingSet();

			combinedBindings = combinedBindings.merge(
					neighborOutgoingConsequentBindingSet.translate(this.getRule().getAntecedent(), neighborMatches));
		}

		combinedBindings = combinedBindings.merge(combinedBindings);

		TripleVarBindingSet compatiblePatternBindingsOnly = combinedBindings.keepCompatible(aBindingSet);

		this.setIncomingAntecedentBindingSet(compatiblePatternBindingsOnly.getFullBindingSet());

	}

	private boolean isEmpty(TripleVarBindingSet aBindingSet) {
		return aBindingSet.isEmpty()
				|| (aBindingSet.getBindings().size() == 1 && !aBindingSet.getBindings().iterator().hasNext());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RuleNode other = (RuleNode) obj;
		if (rule == null) {
			if (other.rule != null)
				return false;
		} else if (!rule.equals(other.rule))
			return false;
		return true;
	}

	public boolean hasAntecedent() {
		return !this.rule.getAntecedent().isEmpty();
	}

	public boolean hasConsequent() {
		return !this.rule.getConsequent().isEmpty();
	}

	@Override
	public String toString() {
		return "ReasonerNode [" + (rule != null ? "rule=" + rule : "") + "]";
	}

}
