package eu.knowledge.engine.reasonerprototype;

import eu.knowledge.engine.reasonerprototype.api.BindingSet;

public interface ReasoningNode {

	boolean plan();

	void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet);

}
