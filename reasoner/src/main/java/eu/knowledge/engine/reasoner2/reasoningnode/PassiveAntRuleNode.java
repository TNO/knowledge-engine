package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public class PassiveAntRuleNode extends AntRuleNode {

	public PassiveAntRuleNode(BaseRule aRule) {
		super(aRule);
	}

	public BindingSet getResultBindingSetInput() {
		return this.resultBindingSetInput.get().getFullBindingSet().toBindingSet();
	}

	public void setFilterBindingSetOutput(BindingSet bs) {
		this.filterBindingSetOutput = new TripleVarBindingSet(this.getRule().getAntecedent(), bs);
	}

	@Override
	public boolean readyForTransformFilter() {
		return false;
	}

	@Override
	public void transformFilterBS() {
		assert false;
	}

	@Override
	public boolean readyForApplyRule() {
		return false;
	}

	@Override
	public void applyRule() {
		assert false;
	}
}
