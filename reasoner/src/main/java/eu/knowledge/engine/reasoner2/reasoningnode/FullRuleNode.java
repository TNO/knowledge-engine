/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.AntSide;
import eu.knowledge.engine.reasoner2.ConsSide;

/**
 * @author nouwtb
 *
 */
public class FullRuleNode extends RuleNode implements AntSide, ConsSide {

	public void convertResultBindingSet() {
		// TODO Manually-generated method stub

	}

	public void convertFilterBindingSet() {
		// TODO Manually-generated method stub

	}

	@Override
	public void addFilterBindingSetInput(TripleVarBindingSet bs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendResultBindingSetOutput(TripleVarBindingSet bs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addResultBindingSetInput(TripleVarBindingSet bs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendFilterBindingSetOutput(TripleVarBindingSet bs) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPartOfLoop() {
		// TODO Auto-generated method stub
		return false;
	}

}
