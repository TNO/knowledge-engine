/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner2.ConsSide;

/**
 * @author nouwtb
 *
 */
public abstract class ConsRuleNode extends RuleNode implements ConsSide {

	private BindingSetStore incomingConsequentBindingSet;
	private TripleVarBindingSet outgoingConsequentBindingSet;

	/**
	 * All relevant rules from the {@link RuleStore} whose antecedents match this
	 * rule's consequent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> consequentNeighbors;

	@Override
	public boolean isPartOfLoop() {
		return false;
	}

	@Override
	public void addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {
		assert this.consequentNeighbors.containsKey(aNeighbor);
		this.incomingConsequentBindingSet.add(aNeighbor, bs);
	}

	@Override
	public TripleVarBindingSet getResultBindingSetOutput() {
		return this.outgoingConsequentBindingSet;
	}

}
