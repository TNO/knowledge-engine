package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner.BaseRule;

/**
 * @author nouwtb
 *
 */
public class PassiveAntRuleNode extends AntRuleNode {

	public PassiveAntRuleNode(BaseRule aRule) {
		super(aRule);
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
