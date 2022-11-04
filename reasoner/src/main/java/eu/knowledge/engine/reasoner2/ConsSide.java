/**
 * 
 */
package eu.knowledge.engine.reasoner2;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner2.reasoningnode.RuleNode;

/**
 * @author nouwtb
 *
 */
public interface ConsSide {

	public void addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs);

	public TripleVarBindingSet getResultBindingSetOutput();
}
