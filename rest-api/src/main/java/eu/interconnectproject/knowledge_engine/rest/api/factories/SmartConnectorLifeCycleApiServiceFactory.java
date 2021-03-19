package eu.interconnectproject.knowledge_engine.rest.api.factories;

import eu.interconnectproject.knowledge_engine.rest.api.SmartConnectorLifeCycleApiService;
import eu.interconnectproject.knowledge_engine.rest.api.impl.SmartConnectorLifeCycleApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class SmartConnectorLifeCycleApiServiceFactory {
    private static final SmartConnectorLifeCycleApiService service = new SmartConnectorLifeCycleApiServiceImpl();

    public static SmartConnectorLifeCycleApiService getSmartConnectorLifeCycleApi() {
        return service;
    }
}
