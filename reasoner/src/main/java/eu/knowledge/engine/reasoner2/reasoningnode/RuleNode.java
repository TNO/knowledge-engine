/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public abstract class RuleNode {

	private BaseRule rule;
	private boolean executeViaTaskboard;

	public BaseRule getRule() {
		return rule;
	}

	public boolean getExecuteViaTaskboard() {
		return executeViaTaskboard;
	}

	public abstract boolean isPartOfLoop();

	public abstract void addRBInput(RuleNode aRuleNode, TripleVarBindingSet aBindingSet);

	public abstract TripleVarBindingSet getFBOutput();

	public abstract void applyRule();

	public abstract void transformFilterBS();

	public abstract TripleVarBindingSet getRBOutput();

	public abstract void addFDBInput(RuleNode aRuleNode, TripleVarBindingSet aBindingSet);

}
