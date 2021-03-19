package eu.interconnectproject.knowledge_engine.rest.api.impl;

import eu.interconnectproject.knowledge_engine.rest.api.*;
import eu.interconnectproject.knowledge_engine.rest.model.Workaround;
import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class KnowledgeInteractionLifeCycleApiServiceImpl extends KnowledgeInteractionLifeCycleApiService {

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scKiPost(@NotNull String knowledgeBaseId, Workaround workaround, SecurityContext securityContext)
			throws NotFoundException {
		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Smart connector not found, because its ID must be a valid URI.").build();
		}
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			return Response.status(404).entity("Smart connector not found, because its ID is unknown.").build();
		}

		if (!restKb.register(workaround)) {
			
		}

		return Response.ok().build();
	}

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
}
