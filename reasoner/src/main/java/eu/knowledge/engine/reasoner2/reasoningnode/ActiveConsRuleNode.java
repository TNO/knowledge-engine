package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.concurrent.ExecutionException;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;

/**
 * @author nouwtb
 *
 */
public class ActiveConsRuleNode extends ConsRuleNode {

	public ActiveConsRuleNode(BaseRule aRule) {
		super(aRule);
	}

	@Override
	public boolean readyForTransformFilter() {
		return true;
	}

	@Override
	public void transformFilterBS() {
		assert this.readyForTransformFilter();
	}

	@Override
	public boolean readyForApplyRule() {
		return this.filterBindingSetInput.haveAllNeighborsContributed();
	}

	@Override
	public void applyRule() {
		assert this.readyForApplyRule();
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getBindingSetHandler();
		try {
			var result = handler.handle(this.filterBindingSetInput.get().toBindingSet()).get();
			this.resultBindingSetOutput = result.toTripleVarBindingSet(this.getRule().getConsequent());
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
