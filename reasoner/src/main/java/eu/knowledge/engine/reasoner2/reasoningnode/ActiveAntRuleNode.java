package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.concurrent.ExecutionException;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;

/**
 * @author nouwtb
 *
 */
public class ActiveAntRuleNode extends AntRuleNode {

	public ActiveAntRuleNode(BaseRule aRule) {
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
		return this.resultBindingSetInput.haveAllNeighborsContributed();
	}

	@Override
	public void applyRule() {
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getSinkBindingSetHandler();
		try {
			handler.handle(this.resultBindingSetInput.get().getFullBindingSet().toBindingSet()).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO
			e.printStackTrace();
		}
	}
}
