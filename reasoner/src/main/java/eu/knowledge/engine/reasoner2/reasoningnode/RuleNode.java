/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner.BaseRule;

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

}
