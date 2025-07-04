package eu.knowledge.engine.reasoner.rulenode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.knowledge.engine.reasoner.AntSide;
import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

/**
 * A rule that only has an antecedent.
 * 
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

	private Set<CombiMatch> antecedentCombiMatches;

	@Override
	public void setAntecedentCombiMatches(Set<CombiMatch> someCombiMatches) {
		this.antecedentCombiMatches = someCombiMatches;

		// also set the combi matches to the rule store.
		this.resultBindingSetInput.setCombiMatches(this.antecedentCombiMatches);
	}

	@Override
	public Set<CombiMatch> getAntecedentCombiMatches() {
		return this.antecedentCombiMatches;
	}

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
	public boolean addResultBindingSetInput(RuleNode aNeighbor, Map<Match, TripleVarBindingSet> someBindingSets) {
		assert (antecedentNeighbours.keySet().contains(aNeighbor));
		Map<Match, TripleVarBindingSet> filteredBS = someBindingSets;
		if (this.filterBindingSetOutput != null && !this.hasProactiveParent(aNeighbor)) {

			for (Map.Entry<Match, TripleVarBindingSet> matchBS : filteredBS.entrySet()) {
				filteredBS.put(matchBS.getKey(), matchBS.getValue().keepCompatible(this.filterBindingSetOutput));
			}
		}

		var changed = this.resultBindingSetInput.add(aNeighbor, filteredBS);
		if (changed && this.filterBindingSetOutput == null && this.hasProactiveParent(aNeighbor)) {
			var previousBindingSetOutput = this.filterBindingSetOutput;
			this.filterBindingSetOutput = this.resultBindingSetInput.get();

			if (!this.filterBindingSetOutput.equals(previousBindingSetOutput))
				this.isFilterBindingSetOutputDirty = true;
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
	public TripleVarBindingSet getResultBindingSetInput() {
		return this.resultBindingSetInput.get();
	}

	@Override
	public Set<RuleNode> getAllSameLoopNeighbors() {
		return new HashSet<>();
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetInput() {
		return null;
	}

	/**
	 * Prints the binding set store of this Rule Node to the std out in a markdown
	 * table format.
	 */
	public void printResultBindingSetInputStore() {
		this.resultBindingSetInput.printDebuggingTable();
	}

}
