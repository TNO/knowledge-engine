/**
 * 
 */
package eu.knowledge.engine.reasoner2;

import java.util.Set;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.reasoningnode.RuleNode;

/**
 * @author nouwtb
 *
 */
public interface AntSide {

	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches);

	public Set<RuleNode> getAntecedentNeighbours();

	public void addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs);

	public TripleVarBindingSet getFilterBindingSetOutput();

}
