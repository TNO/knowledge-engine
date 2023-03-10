package eu.knowledge.engine.reasoner.rulenode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * Active means that it has a bindingsethandler and can be applied.
 * 
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
	public Future<Void> applyRule() {
		assert this.getRule() instanceof Rule;

		var handler = ((Rule) this.getRule()).getSinkBindingSetHandler();
		TripleVarBindingSet fullBindingSet = this.resultBindingSetInput.get().getFullBindingSet();

		CompletableFuture<Void> f;
		if (!fullBindingSet.isEmpty()) {
			f = handler.handle(fullBindingSet.toBindingSet());
		} else {
			f = new CompletableFuture<Void>();
			f.complete(null);
		}
		return f;
	}
}
