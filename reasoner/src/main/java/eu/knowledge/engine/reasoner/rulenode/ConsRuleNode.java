package eu.knowledge.engine.reasoner.rulenode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.ConsSide;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

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

	@Override
	public TripleVarBindingSet getResultBindingSetInput() {
		return null;
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetInput() {
		return this.filterBindingSetInput.get();
	}

	@Override
	public Set<RuleNode> getAllSameLoopNeighbors() {
		return new HashSet<>();
	}

	/**
	 * Prints the binding set store of this Rule Node to the std out in a markdown
	 * table format.
	 */
	public void printInputBindingSetStore() {
		this.filterBindingSetInput.printDebuggingTable();
	}

}
