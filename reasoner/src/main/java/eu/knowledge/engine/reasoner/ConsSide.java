package eu.knowledge.engine.reasoner;

import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;

/**
 * @author nouwtb
 *
 */
public interface ConsSide {

	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches);

	public Map<RuleNode, Set<Match>> getConsequentNeighbours();
	
	public boolean addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs);
}
