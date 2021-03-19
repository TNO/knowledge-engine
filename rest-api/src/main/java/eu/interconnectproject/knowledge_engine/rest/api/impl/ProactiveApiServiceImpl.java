package eu.interconnectproject.knowledge_engine.rest.api.impl;

import eu.interconnectproject.knowledge_engine.rest.api.*;
import eu.interconnectproject.knowledge_engine.rest.model.*;

import eu.interconnectproject.knowledge_engine.rest.model.AskResult;
import java.util.List;
import java.util.Map;
import eu.interconnectproject.knowledge_engine.rest.model.PostResult;

import java.util.List;
import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class ProactiveApiServiceImpl extends ProactiveApiService {
	
	private RestKnowledgeBaseManager store = RestKnowledgeBaseManager.newInstance();
	
    @Override
    public Response scAskPost( @NotNull String knowledgeBaseId,  @NotNull String knowledgeInteractionId, List<Map<String, String>> requestBody, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response scPostPost( @NotNull String knowledgeBaseId,  @NotNull String knowledgeInteractionId, List<Map<String, String>> requestBody, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
