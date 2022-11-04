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
	public boolean isPartOfLoop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {
		// TODO Auto-generated method stub

	}

	@Override
	public TripleVarBindingSet getResultBindingSetOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {
		// TODO Auto-generated method stub

	}

	@Override
	public TripleVarBindingSet getFilterBindingSetOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addRBInput(RuleNode aRuleNode, TripleVarBindingSet aBindingSet) {
		// TODO Auto-generated method stub

	}

	@Override
	public TripleVarBindingSet getFBOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void applyRule() {
		// TODO Auto-generated method stub

	}

	@Override
	public void transformFilterBS() {
		// TODO Auto-generated method stub

	}

	@Override
	public TripleVarBindingSet getRBOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFDBInput(RuleNode aRuleNode, TripleVarBindingSet aBindingSet) {
		// TODO Auto-generated method stub

	}

}
