package eu.interconnectproject.knowledge_engine.rest.api.impl;

import eu.interconnectproject.knowledge_engine.rest.api.*;
import eu.interconnectproject.knowledge_engine.rest.model.Workaround;
import eu.interconnectproject.knowledge_engine.rest.model.WorkaroundWithId;
import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

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

		String kiId = restKb.register(workaround);
		if (kiId == null) {
			return Response.status(400).entity("The registration of the knowledge interaction failed, probably because the request was invalid.").build();
		}

		return Response.ok().entity(kiId).build();
	}

	@Override
	public Response scKiDelete(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			return Response.status(404).entity("Smart connector not found, because its ID is unknown.").build();
		}

		if (!restKb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			return Response.status(404).entity("Smart connector found, but the given knowledge interaction ID is unknown.").build();
		}

		restKb.delete(knowledgeInteractionId);

		return Response.ok().build();
	}

	@Override
	public Response scKiGet(@NotNull String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			return Response.status(404).entity("Smart connector not found, because its ID is unknown.").build();
		}

		Set<WorkaroundWithId> kis = restKb.getKnowledgeInteractions();

		return Response.ok().entity(kis).build();
	}
}
