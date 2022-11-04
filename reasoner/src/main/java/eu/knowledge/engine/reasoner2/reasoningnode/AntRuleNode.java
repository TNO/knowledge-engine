package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner2.AntSide;

/**
 * @author nouwtb
 *
 */
public abstract class AntRuleNode extends RuleNode implements AntSide {

	public AntRuleNode(BaseRule aRule) {
		super(aRule);
		this.resultBindingSetInput = new BindingSetStore(aRule.getAntecedent(), this.antecedentNeighbours.keySet());
	}

	protected BindingSetStore resultBindingSetInput;
	private TripleVarBindingSet filterBindingSetOutput;

	/**
	 * All relevant rules from the {@link RuleStore} whose consequents match this
	 * rule's antecedent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> antecedentNeighbours = new HashMap<>();

	@Override
	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches) {
		this.antecedentNeighbours.put(neighbour, matches);
	}

	@Override
	public Set<RuleNode> getAntecedentNeighbours() {
		return this.antecedentNeighbours.keySet();
	}
	
	@Override
	public Set<RuleNode> getAllNeighbours() {
		return this.getAntecedentNeighbours();
	}

	@Override
	public boolean addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet aBindingSet) {
		assert (antecedentNeighbours.keySet().contains(aNeighbor));
		return this.resultBindingSetInput.add(aNeighbor, aBindingSet);
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetOutput() {
		return this.filterBindingSetOutput;
	}

	@Override
	public TripleVarBindingSet getResultBindingSetOutput() {
		return null;
	}
}
