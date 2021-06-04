package eu.interconnectproject.knowledge_engine.rest.api.client_example;

import eu.interconnectproject.knowledge_engine.rest.model.HandleRequest;
import eu.interconnectproject.knowledge_engine.rest.model.HandleResponse;

public interface KnowledgeHandler {
	public HandleResponse handle(HandleRequest knowledgeRequest);
}
