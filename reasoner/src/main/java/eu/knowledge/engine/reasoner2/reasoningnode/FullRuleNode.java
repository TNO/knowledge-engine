package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.AntSide;
import eu.knowledge.engine.reasoner2.ConsSide;

/**
 * @author nouwtb
 *
 */
public class FullRuleNode extends RuleNode implements AntSide, ConsSide {

	private BindingSetStore resultBindingSetInput;
	private BindingSetStore filterBindingSetInput;
	private TripleVarBindingSet resultBindingSetOutput;
	private TripleVarBindingSet filterBindingSetOutput;

	public FullRuleNode(BaseRule aRule) {
		super(aRule);
		this.resultBindingSetInput = new BindingSetStore(aRule.getAntecedent(), this.antecedentNeighbours.keySet());
		this.filterBindingSetInput = new BindingSetStore(aRule.getConsequent(), this.consequentNeighbours.keySet());
	}

	private Map<RuleNode, Set<Match>> antecedentNeighbours = new HashMap<>();
	private Map<RuleNode, Set<Match>> consequentNeighbours = new HashMap<>();

	@Override
	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches) {
		this.consequentNeighbours.put(neighbour, matches);
	}

	@Override
	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches) {
		this.antecedentNeighbours.put(neighbour, matches);
	}

	@Override
	public Map<RuleNode, Set<Match>> getConsequentNeighbours() {
		return this.consequentNeighbours;
	}

	@Override
	public Map<RuleNode, Set<Match>> getAntecedentNeighbours() {
		return this.antecedentNeighbours;
	}

	@Override
	public Set<RuleNode> getAllNeighbours() {
		Set<RuleNode> result = new HashSet<>();

		result.addAll(this.getAntecedentNeighbours().keySet());
		result.addAll(this.getConsequentNeighbours().keySet());

		return result;
	}

	@Override
	public boolean addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {
		return this.filterBindingSetInput.add(aNeighbor, bs);
	}

	@Override
	public boolean addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {

		TripleVarBindingSet filteredBS = bs;
		if (this.filterBindingSetOutput != null) {
			filteredBS = bs.keepCompatible(this.filterBindingSetOutput);
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
		return this.resultBindingSetOutput;
	}

	@Override
	public boolean readyForTransformFilter() {
		// TODO: This (the "Except" part) was needed to make transitivity work, but not
		// sure if it is correct
		return this.filterBindingSetInput.haveAllNeighborsContributedExcept(this);
	}

	@Override
	public void transformFilterBS() {
		assert this.readyForTransformFilter();
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getInverseBindingSetHandler();
		try {
			var result = handler.handle(this.filterBindingSetInput.get().toBindingSet()).get();
			this.filterBindingSetOutput = result.toTripleVarBindingSet(this.getRule().getAntecedent());
		} catch (InterruptedException | ExecutionException e) {
			// TODO
			e.printStackTrace();
		}
	}

	@Override
	public boolean readyForApplyRule() {
		// TODO: This (the "Except" part) was needed to make transitivity work, but not
		// sure if it is correct
		return this.resultBindingSetInput.haveAllNeighborsContributedExcept(this);
	}

	@Override
	public void applyRule() {
		assert this.readyForApplyRule();
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getBindingSetHandler();
		try {
			TripleVarBindingSet fullBindingSet = this.resultBindingSetInput.get().getFullBindingSet();
			if (!fullBindingSet.isEmpty()) {
				var result = handler.handle(fullBindingSet.toBindingSet()).get();
				this.resultBindingSetOutput = result.toTripleVarBindingSet(this.getRule().getConsequent());
			}
		} catch (InterruptedException | ExecutionException e) {
			// TODO
			e.printStackTrace();
		}
	}
}
