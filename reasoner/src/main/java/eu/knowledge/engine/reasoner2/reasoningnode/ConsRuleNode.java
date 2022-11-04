package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner2.ConsSide;

/**
 * @author nouwtb
 *
 */
public abstract class ConsRuleNode extends RuleNode implements ConsSide {

	public ConsRuleNode(BaseRule aRule) {
		super(aRule);
		this.filterBindingSetInput = new BindingSetStore(aRule.getConsequent(), this.consequentNeighbours.keySet());
	}

	protected BindingSetStore filterBindingSetInput;
	protected TripleVarBindingSet resultBindingSetOutput;

	/**
	 * All relevant rules from the {@link RuleStore} whose antecedents match this
	 * rule's consequent either fully or partially.
	 */
	private Map<RuleNode, Set<Match>> consequentNeighbours = new HashMap<>();

	@Override
	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches) {
		this.consequentNeighbours.put(neighbour, matches);
	}

	@Override
	public Map<RuleNode, Set<Match>> getConsequentNeighbours() {
		return this.consequentNeighbours;
	}

	@Override
	public Set<RuleNode> getAllNeighbours() {
		return this.getConsequentNeighbours().keySet();
	}

	@Override
	public boolean addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {
		assert this.consequentNeighbours.containsKey(aNeighbor);
		return this.filterBindingSetInput.add(aNeighbor, bs);
	}

	@Override
	public TripleVarBindingSet getResultBindingSetOutput() {
		return this.resultBindingSetOutput;
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetOutput() {
		return null;
	}
}
