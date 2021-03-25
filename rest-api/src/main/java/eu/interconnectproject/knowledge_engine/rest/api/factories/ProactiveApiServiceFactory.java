package eu.interconnectproject.knowledge_engine.rest.api.factories;

import eu.interconnectproject.knowledge_engine.rest.api.ProactiveApiService;
import eu.interconnectproject.knowledge_engine.rest.api.impl.ProactiveApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class ProactiveApiServiceFactory {
    private static final ProactiveApiService service = new ProactiveApiServiceImpl();

    public static ProactiveApiService getProactiveApi() {
        return service;
    }
}
