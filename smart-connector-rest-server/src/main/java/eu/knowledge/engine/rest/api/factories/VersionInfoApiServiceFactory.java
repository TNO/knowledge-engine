package eu.knowledge.engine.rest.api.factories;

import eu.knowledge.engine.rest.api.VersionInfoApiService;
import eu.knowledge.engine.rest.api.impl.VersionInfoApiServiceImpl;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2022-05-13T16:52:43.224496100+01:00[Europe/Amsterdam]")
public class VersionInfoApiServiceFactory {
    private static final VersionInfoApiService service = new VersionInfoApiServiceImpl();

    public static VersionInfoApiService getVersionInfoApi() {
        return service;
    }
}
