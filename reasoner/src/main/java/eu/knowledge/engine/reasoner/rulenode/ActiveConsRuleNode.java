package eu.knowledge.engine.reasoner.rulenode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;

/**
 * Active means it has a bindingsethandler and can be applied.
 * 
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
	public Future<Void> applyRule() {
		assert this.readyForApplyRule();
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getBindingSetHandler();

		var previousBindingSetOutput = this.resultBindingSetOutput;

		return handler.handle(this.filterBindingSetInput.get().toBindingSet()).thenAccept(result -> {
			this.resultBindingSetOutput = result.toTripleVarBindingSet(this.getRule().getConsequent());
			if (!this.resultBindingSetOutput.equals(previousBindingSetOutput))
				this.isResultBindingSetOutputDirty = true;
		});
	}

	@Override
	public boolean shouldPropagateFilterBindingSetOutput() {
		return false;
	}

	@Override
	public boolean shouldPropagateResultBindingSetOutput() {
		return this.isResultBindingSetOutputDirty;
	}
}
