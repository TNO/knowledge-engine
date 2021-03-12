package eu.interconnectproject.knowlege_engine.rest.api.factories;

import eu.interconnectproject.knowlege_engine.rest.api.ScApiService;
import eu.interconnectproject.knowlege_engine.rest.api.impl.ScApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-12T14:24:41.746+01:00[Europe/Berlin]")
public class ScApiServiceFactory {
    private static final ScApiService service = new ScApiServiceImpl();

    public static ScApiService getScApi() {
        return service;
    }
}
