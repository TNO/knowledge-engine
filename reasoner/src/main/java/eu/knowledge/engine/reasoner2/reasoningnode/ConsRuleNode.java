/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner2.ConsSide;

/**
 * @author nouwtb
 *
 */
public abstract class ConsRuleNode extends RuleNode implements ConsSide {

	@Override
	public boolean isPartOfLoop() {
		return false;
	}

}
