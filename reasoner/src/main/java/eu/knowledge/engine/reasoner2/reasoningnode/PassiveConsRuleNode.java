package eu.knowledge.engine.reasoner2.reasoningnode;


import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

public class PassiveConsRuleNode extends ConsRuleNode {

	public PassiveConsRuleNode(BaseRule aRule) {
		super(aRule);
	}

	public void setResultBindingOutput(BindingSet bs) {
		assert !this.getRule().getConsequent().isEmpty();
		this.resultBindingSetOutput = new TripleVarBindingSet(this.getRule().getConsequent(), bs);
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
