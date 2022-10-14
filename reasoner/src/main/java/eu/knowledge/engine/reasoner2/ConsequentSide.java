/**
 * 
 */
package eu.knowledge.engine.reasoner2;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public interface ConsequentSide {

	public void addFilterBindingSetInput(TripleVarBindingSet bs);

	public void sendResultBindingSetOutput(TripleVarBindingSet bs);
}
