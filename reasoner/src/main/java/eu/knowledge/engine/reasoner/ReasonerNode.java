package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

public class ReasonerNode {

	/**
	 * The rule node from which this reasoner node has been derived.
	 */
	private Rule rule;

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
	private Map<ReasonerNode, Set<Match>> antecedentNeighbors;

	/**
	 * All relevant rules from the {@link RuleStore} whose antecedents match this
	 * rule's consequent either fully or partially.
	 */
	private Map<ReasonerNode, Set<Match>> consequentNeighbors;

	/**
	 * Create a new ReasonerNode for the rule of the given {@code aRule}.
	 * 
	 * @param aRule The rule of which to create a reasoner node.
	 */
	public ReasonerNode(Rule aRule) {
		this.antecedentNeighbors = new HashMap<>();
		this.consequentNeighbors = new HashMap<>();
		this.rule = aRule;
	}

	public Rule getRule() {
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

	public Map<ReasonerNode, Set<Match>> getAntecedentNeighbors() {
		assert !this.getRule().getAntecedent().isEmpty();
		return antecedentNeighbors;
	}

	public void addAntecedentNeighbor(ReasonerNode aReasonerNode, Set<Match> someMatches) {
		if (this.antecedentNeighbors.put(aReasonerNode, someMatches) == null) {
			aReasonerNode.addConsequentNeighbor(this, Match.invert(someMatches));
		}
	}

	public void addConsequentNeighbor(ReasonerNode aReasonerNode, Set<Match> someMatches) {
		if (this.consequentNeighbors.put(aReasonerNode, someMatches) == null) {
			aReasonerNode.addAntecedentNeighbor(this, Match.invert(someMatches));
		}

	}

	public Map<ReasonerNode, Set<Match>> getConsequentNeighbors() {
		assert !this.getRule().getConsequent().isEmpty();
		return consequentNeighbors;
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
		ReasonerNode other = (ReasonerNode) obj;
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
