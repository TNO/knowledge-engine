/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.Set;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public class ActiveConsRuleNode extends ConsRuleNode {

	public void query() {
		// TODO Manually-generated method stub

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

	@Override
	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<RuleNode> getConsequentNeighbours() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<RuleNode> getAllNeighbours() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean readyForTransformFilter() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean readyForApplyRule() {
		// TODO Auto-generated method stub
		return false;
	}

}
