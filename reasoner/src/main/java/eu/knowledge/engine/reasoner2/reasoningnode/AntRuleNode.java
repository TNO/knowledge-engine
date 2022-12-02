package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.HashMap;
import java.util.HashSet;
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

	protected TripleVarBindingSet filterBindingSetOutput;

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
	public Map<RuleNode, Set<Match>> getAntecedentNeighbours() {
		return this.antecedentNeighbours;
	}

	@Override
	public Set<RuleNode> getAllNeighbours() {
		return this.getAntecedentNeighbours().keySet();
	}

	@Override
	public boolean addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet aBindingSet) {
		assert (antecedentNeighbours.keySet().contains(aNeighbor));
		TripleVarBindingSet filteredBS = aBindingSet;
		if (this.filterBindingSetOutput != null) {
			filteredBS = aBindingSet.keepCompatible(this.filterBindingSetOutput);
		}

		var changed = this.resultBindingSetInput.add(aNeighbor, filteredBS);
		if (changed && this.filterBindingSetOutput == null) {
			this.filterBindingSetOutput = this.resultBindingSetInput.get();
		}

		return changed;
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetOutput() {
		return this.filterBindingSetOutput;
	}

	@Override
	public TripleVarBindingSet getResultBindingSetOutput() {
		return null;
	}

	@Override
	public Set<RuleNode> getAllSameLoopNeighbors() {
		return new HashSet<>();
	}
}
