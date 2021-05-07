package eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.factories;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.MessagingApiService;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.impl.MessagingApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-04-30T11:30:55.605099+02:00[Europe/Amsterdam]")
public class MessagingApiServiceFactory {
    private static final MessagingApiService service = new MessagingApiServiceImpl();

    public static MessagingApiService getMessagingApi() {
        return service;
    }
}
