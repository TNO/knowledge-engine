/**
 * 
 */
package eu.knowledge.engine.reasoner2;

import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public interface AntecedentSide {

	public void addResultBindingSetInput(TripleVarBindingSet bs);

	public void sendFilterBindingSetOutput(TripleVarBindingSet bs);

}
