/**
 * 
 */
package eu.knowledge.engine.reasoner2.reasoningnode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.AntSide;
import eu.knowledge.engine.reasoner2.ConsSide;

/**
 * @author nouwtb
 *
 */
public class FullRuleNode extends RuleNode implements AntSide, ConsSide {

	private Map<RuleNode, Set<Match>> antecedentNeighbours;
	private Map<RuleNode, Set<Match>> consequentNeighbours;

	@Override
	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches) {
		// TODO Auto-generated method stub	
	}

	@Override
	public Set<RuleNode> getConsequentNeighbours() {
		return this.consequentNeighbours.keySet();
	}
	
	@Override
	public Set<RuleNode> getAntecedentNeighbours() {
		return this.antecedentNeighbours.keySet();
	}

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

	@Override
	public Set<RuleNode> getAllNeighbours() {
		Set<RuleNode> result = new HashSet<>();

		result.addAll(this.getAntecedentNeighbours());
		result.addAll(this.getConsequentNeighbours());

		return result;
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
