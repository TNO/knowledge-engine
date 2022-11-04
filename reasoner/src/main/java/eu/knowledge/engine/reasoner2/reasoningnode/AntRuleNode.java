package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner2.AntSide;

/**
 * @author nouwtb
 *
 */
public abstract class AntRuleNode extends RuleNode implements AntSide {

	private BindingSetStore incomingAntecedentBindingSet;
	private TripleVarBindingSet outgoingAntecedentBindingSet;

	/**
	 * All relevant rules from the {@link RuleStore} whose consequents match this
	 * rule's antecedent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> antecedentNeighbors;

	@Override
	public Set<RuleNode> getAntecedentNeighbours() {
		return this.antecedentNeighbors.keySet();
	}
	
	
	@Override
	public Set<RuleNode> getAllNeighbours() {
		return this.antecedentNeighbors.keySet();
	}

	@Override
	public boolean isPartOfLoop() {
		return false;
	}

	@Override
	public void addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet aBindingSet) {
		assert (antecedentNeighbors.keySet().contains(aNeighbor));
		this.incomingAntecedentBindingSet.add(aNeighbor, aBindingSet);
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetOutput() {
		return this.outgoingAntecedentBindingSet;
	}

}
