package eu.knowledge.engine.reasoner;

import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulenode.RuleNode;

/**
 * @author nouwtb
 *
 */
public interface AntSide {

	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches);

	public Map<RuleNode, Set<Match>> getAntecedentNeighbours();

	public boolean addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs);

}
