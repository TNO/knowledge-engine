package eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.factories;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.SmartConnectorManagementApiService;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.impl.SmartConnectorManagementApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-04-30T11:30:55.605099+02:00[Europe/Amsterdam]")
public class SmartConnectorManagementApiServiceFactory {
    private static final SmartConnectorManagementApiService service = new SmartConnectorManagementApiServiceImpl();

    public static SmartConnectorManagementApiService getSmartConnectorManagementApi() {
        return service;
    }
}
