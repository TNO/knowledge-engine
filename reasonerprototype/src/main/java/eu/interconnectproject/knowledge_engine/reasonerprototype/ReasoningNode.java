package eu.interconnectproject.knowledge_engine.reasonerprototype;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;

public interface ReasoningNode {

	void processResultingBindingSet(ReasoningNode child, BindingSet bindingSet);

}
