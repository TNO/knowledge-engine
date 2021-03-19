package eu.interconnectproject.knowledge_engine.rest.api.impl;

import eu.interconnectproject.knowledge_engine.rest.api.*;
import eu.interconnectproject.knowledge_engine.rest.model.*;

import eu.interconnectproject.knowledge_engine.rest.model.Workaround;

import java.util.List;
import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class KnowledgeInteractionLifeCycleApiServiceImpl extends KnowledgeInteractionLifeCycleApiService {

	private SmartConnectorStore store = SmartConnectorStore.newInstance();

	@Override
	public Response scKiDelete(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
	}

	@Override
	public Response scKiGet(@NotNull String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		// do some magic!
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
	}

	@Override
	public Response scKiPost(@NotNull String knowledgeBaseId, Workaround workaround, SecurityContext securityContext)
			throws NotFoundException {
		// do some magic!
		return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
	}
}
