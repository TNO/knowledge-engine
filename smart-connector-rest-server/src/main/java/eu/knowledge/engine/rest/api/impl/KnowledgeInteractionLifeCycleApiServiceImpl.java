package eu.knowledge.engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.lang.arq.TokenMgrError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.KnowledgeInteractionLifeCycleApiService;
import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.model.KnowledgeInteractionBase;
import eu.knowledge.engine.rest.model.KnowledgeInteractionWithId;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class KnowledgeInteractionLifeCycleApiServiceImpl extends KnowledgeInteractionLifeCycleApiService {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeInteractionLifeCycleApiServiceImpl.class);
	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scKiPost(String knowledgeBaseId, KnowledgeInteractionBase knowledgeInteraction, SecurityContext securityContext)
			throws NotFoundException {
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
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				return Response.status(Status.NOT_FOUND).entity(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
					.build();
			} else {
				return Response.status(Status.NOT_FOUND).entity("Could not add knowledge interaction, because the given knowledge base ID is unknown.").build();
			}
		}

		String kiId;
		try {
			kiId = restKb.register(knowledgeInteraction);
		} catch (IllegalArgumentException e) {
			return Response.status(400).entity(e.getMessage()).build();
		} catch (QueryParseException e) {
			var msg = e.getMessage();
			// If this is a Jena error about prefixes, enrich the message with a KE-specific note.
			if (msg.contains("prefix")) {
				msg = msg + ". Note: Have you included your prefixes in the 'prefixes' property?";
			}
			return Response.status(400).entity("Invalid graph pattern: " + msg).build();
		} catch (TokenMgrError e) {
			return Response.status(400).entity("Invalid graph pattern: " + e.getMessage()).build();
		}
		if (kiId == null) {
			return Response.status(500).entity(
					"The registration of the knowledge interaction failed, because of an internal error.")
					.build();
		}

		LOG.info("Knowledge interaction created in KB {}: {} (issued id: {})", knowledgeBaseId, knowledgeInteraction, kiId);
		return Response.ok().entity(kiId).build();
	}

	@Override
	public Response scKiDelete(String knowledgeBaseId, String knowledgeInteractionId,
			SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				return Response.status(Status.NOT_FOUND).entity(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
					.build();
			} else {
				return Response.status(Status.NOT_FOUND).entity("Knowledge base not found, because its knowledge base ID is unknown.").build();
			}
		}

		if (knowledgeInteractionId == null) {
			return Response.status(400).entity("Missing valid Knowledge-Interaction-Id header.").build();
		}
		
		if (!restKb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			return Response.status(Status.NOT_FOUND)
					.entity("Knowledge base found, but the given knowledge interaction knowledge base ID is unknown.").build();
		}

		restKb.delete(knowledgeInteractionId);

		LOG.info("Knowledge interaction deleted in KB {}: {}", knowledgeBaseId, knowledgeInteractionId);

		return Response.ok().build();
	}

	@Override
	public Response scKiGet(String knowledgeBaseId, SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				return Response.status(Status.NOT_FOUND).entity(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
					.build();
			} else {
				return Response.status(Status.NOT_FOUND).entity("Knowledge base not found, because its knowledge base ID is unknown.").build();
			}
		}

		Set<KnowledgeInteractionWithId> kis = restKb.getKnowledgeInteractions();

		return Response.ok().entity(kis).build();
	}
}
