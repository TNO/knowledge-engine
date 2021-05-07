package eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.impl;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.*;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.*;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.KnowledgeEngineRuntimeDetails;

import java.util.List;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-04-30T11:30:55.605099+02:00[Europe/Amsterdam]")
public class SmartConnectorManagementApiServiceImpl extends SmartConnectorManagementApiService {
    @Override
    public Response runtimedetailsGet(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response runtimedetailsPost(KnowledgeEngineRuntimeDetails knowledgeEngineRuntimeDetails, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
