package eu.knowledge.engine.rest.api.client_example;

import eu.knowledge.engine.rest.model.HandleRequest;
import eu.knowledge.engine.rest.model.HandleResponse;

public interface KnowledgeHandler {
	public HandleResponse handle(HandleRequest knowledgeRequest);
}
