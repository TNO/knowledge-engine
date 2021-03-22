package eu.interconnectproject.knowledge_engine.rest.api.client_example;

import eu.interconnectproject.knowledge_engine.rest.model.InlineObject1;
import eu.interconnectproject.knowledge_engine.rest.model.InlineResponse200;

public interface KnowledgeHandler {
	public InlineObject1 handle(InlineResponse200 knowledgeRequest);
}
