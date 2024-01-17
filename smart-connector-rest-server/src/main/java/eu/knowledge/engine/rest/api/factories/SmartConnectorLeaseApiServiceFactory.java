package eu.knowledge.engine.rest.api.factories;

import eu.knowledge.engine.rest.api.SmartConnectorLeaseApiService;
import eu.knowledge.engine.rest.api.impl.SmartConnectorLeaseApiServiceImpl;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class SmartConnectorLeaseApiServiceFactory {
    private static final SmartConnectorLeaseApiService service = new SmartConnectorLeaseApiServiceImpl();

    public static SmartConnectorLeaseApiService getSmartConnectorLeaseApi() {
        return service;
    }
}
