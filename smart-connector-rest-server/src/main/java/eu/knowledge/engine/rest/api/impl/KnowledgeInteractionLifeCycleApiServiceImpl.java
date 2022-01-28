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
import eu.knowledge.engine.rest.model.KnowledgeInteractionId;
import eu.knowledge.engine.rest.model.KnowledgeInteractionWithId;
import eu.knowledge.engine.rest.model.ResponseMessage;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2021-03-16T16:55:43.224496100+01:00[Europe/Amsterdam]")
public class KnowledgeInteractionLifeCycleApiServiceImpl extends KnowledgeInteractionLifeCycleApiService {

	private static final Logger LOG = LoggerFactory.getLogger(KnowledgeInteractionLifeCycleApiServiceImpl.class);
	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scKiPost(String knowledgeBaseId, KnowledgeInteractionBase knowledgeInteraction, SecurityContext securityContext)
			throws NotFoundException {
		if (knowledgeBaseId == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Missing valid Knowledge-Base-Id header.");
			return Response.status(400).entity(response).build();
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge base not found, because its knowledge base ID must be a valid URI.");
			return Response.status(400).entity(response)
					.build();
		}
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.");
				return Response.status(Status.NOT_FOUND).entity(
						response)
					.build();
			} else {
				LOG.info("Someone tried to add a KI to KB {}, but it does not exist.", knowledgeBaseId);
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage(String.format("Could not add knowledge interaction, because the given knowledge base ID (%s) is unknown.", knowledgeBaseId));
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}

		String kiId;
		try {
			kiId = restKb.register(knowledgeInteraction);
		} catch (IllegalArgumentException e) {
			var resposne = new ResponseMessage();
			resposne.setMessageType("error");
			resposne.setMessage(e.getMessage());
			return Response.status(400).entity(resposne).build();
		} catch (QueryParseException e) {
			var msg = e.getMessage();
			// If this is a Jena error about prefixes, enrich the message with a KE-specific note.
			if (msg.contains("prefix")) {
				msg = msg + ". Note: Have you included your prefixes in the 'prefixes' property?";
			}
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Invalid graph pattern: " + msg);
			return Response.status(400).entity(response).build();
		} catch (TokenMgrError e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Invalid graph pattern: " + e.getMessage());
			return Response.status(400).entity(response).build();
		}
		if (kiId == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("The registration of the knowledge interaction failed, because of an internal error.");
			return Response.status(500).entity(
					response)
					.build();
		}

		LOG.info("Knowledge interaction created in KB {}: {} (issued id: {})", knowledgeBaseId, knowledgeInteraction, kiId);
		KnowledgeInteractionId kii = new KnowledgeInteractionId();
		kii.setKnowledgeInteractionId(kiId);
		return Response.ok().entity(kii).build();
	}

	@Override
	public Response scKiDelete(String knowledgeBaseId, String knowledgeInteractionId,
			SecurityContext securityContext) throws NotFoundException {
		var restKb = manager.getKB(knowledgeBaseId);

		if (restKb == null) {
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.");
				return Response.status(Status.NOT_FOUND).entity(
						response)
					.build();
			} else {
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Knowledge base not found, because its knowledge base ID is unknown.");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}

		if (knowledgeInteractionId == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Missing valid Knowledge-Interaction-Id header.");
			return Response.status(400).entity(response).build();
		}
		
		if (!restKb.hasKnowledgeInteraction(knowledgeInteractionId)) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge base found, but the given knowledge interaction knowledge base ID is unknown.");
			return Response.status(Status.NOT_FOUND)
					.entity(response).build();
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
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.");
				return Response.status(Status.NOT_FOUND).entity(
						response)
					.build();
			} else {
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Knowledge base not found, because its knowledge base ID is unknown.");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}

		Set<KnowledgeInteractionWithId> kis = restKb.getKnowledgeInteractions();

		return Response.ok().entity(kis).build();
	}
}
