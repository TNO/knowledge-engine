/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner2.AntSide;

/**
 * @author nouwtb
 *
 */
public abstract class AntRuleNode extends RuleNode implements AntSide {

	@Override
	public boolean isPartOfLoop() {
		return false;
	}

}
