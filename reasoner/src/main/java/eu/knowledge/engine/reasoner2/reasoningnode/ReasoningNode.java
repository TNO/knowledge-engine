/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner.BaseRule;

/**
 * @author nouwtb
 *
 */
public abstract class ReasoningNode {

	private BaseRule rule;
	private boolean executeViaTaskboard;

	public BaseRule getRule() {
		return rule;
	}

	public boolean getExecuteViaTaskboard() {
		return executeViaTaskboard;
	}

}
