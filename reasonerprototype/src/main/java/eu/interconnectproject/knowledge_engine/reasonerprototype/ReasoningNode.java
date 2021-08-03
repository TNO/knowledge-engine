package eu.interconnectproject.knowledge_engine.reasonerprototype;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;

public interface ReasoningNode {

	boolean plan();

	void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet);

}
