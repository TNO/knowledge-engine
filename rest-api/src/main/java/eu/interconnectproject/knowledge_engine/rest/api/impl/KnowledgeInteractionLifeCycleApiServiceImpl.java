package eu.interconnectproject.knowledge_engine.rest.api.impl;

import eu.interconnectproject.knowledge_engine.rest.api.*;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteraction;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionWithId;
import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.*;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class KnowledgeInteractionLifeCycleApiServiceImpl extends KnowledgeInteractionLifeCycleApiService {

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveApiServiceImpl.class);
	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scKiPost(@NotNull String knowledgeBaseId, KnowledgeInteraction workaround, SecurityContext securityContext)
			throws NotFoundException {
		LOG.info("scKiPost called: {}", workaround);

		if (knowledgeBaseId == null) {
			return Response.status(400).entity("Missing valid Knowledge-Base-Id header.").build();
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			return Response.status(400).entity("Knowledge base not found, because its knowledge base ID must be a valid URI.")
					.build();
		}
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			return Response.status(404).entity("Could not add knowledge interaction, because the given knowledge base ID is unknown.").build();
		}

		String kiId;
		try {
			kiId = restKb.register(workaround);
		} catch (IllegalArgumentException e) {
			return Response.status(400).entity(e.getMessage()).build();
		}
		if (kiId == null) {
			return Response.status(500).entity(
					"The registration of the knowledge interaction failed, because of an internal error.")
					.build();
		}

		return Response.ok().entity(kiId).build();
	}

	@Override
	public Response scKiDelete(@NotNull String knowledgeBaseId, @NotNull String knowledgeInteractionId,
			SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			return Response.status(404).entity("Knowledge base not found, because its knowledge base ID is unknown.").build();
		}

		if (!restKb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			return Response.status(404)
					.entity("Knowledge base found, but the given knowledge interaction knowledge base ID is unknown.").build();
		}

		restKb.delete(knowledgeInteractionId);

		return Response.ok().build();
	}

	@Override
	public Response scKiGet(@NotNull String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			return Response.status(404).entity("Smart connector not found, because its KB ID is unknown.").build();
		}

		Set<KnowledgeInteractionWithId> kis = restKb.getKnowledgeInteractions();

		return Response.ok().entity(kis).build();
	}
}
