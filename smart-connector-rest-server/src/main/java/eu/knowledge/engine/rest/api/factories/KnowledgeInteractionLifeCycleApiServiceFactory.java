package eu.knowledge.engine.rest.api.factories;

import eu.knowledge.engine.rest.api.KnowledgeInteractionLifeCycleApiService;
import eu.knowledge.engine.rest.api.impl.KnowledgeInteractionLifeCycleApiServiceImpl;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class KnowledgeInteractionLifeCycleApiServiceFactory {
    private static final KnowledgeInteractionLifeCycleApiService service = new KnowledgeInteractionLifeCycleApiServiceImpl();

    public static KnowledgeInteractionLifeCycleApiService getKnowledgeInteractionLifeCycleApi() {
        return service;
    }
}
