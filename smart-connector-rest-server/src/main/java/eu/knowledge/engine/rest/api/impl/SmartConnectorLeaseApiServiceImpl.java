package eu.knowledge.engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Response.Status;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.api.SmartConnectorLeaseApiService;
import eu.knowledge.engine.rest.model.ResponseMessage;

public class SmartConnectorLeaseApiServiceImpl extends SmartConnectorLeaseApiService {

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scLeaseRenewPut(@NotNull String knowledgeBaseId, SecurityContext securityContext)
			throws NotFoundException {
		if (knowledgeBaseId == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Missing valid Knowledge-Base-Id header.");
			return Response.status(Status.BAD_REQUEST).entity(response).build();
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge base not found, because its knowledge base ID must be a valid URI.");
			return Response.status(Status.BAD_REQUEST).entity(response).build();
		}

		var restKb = manager.getKB(knowledgeBaseId);
		if (restKb == null) {
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			} else {
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Knowledge base not found. (Has its lease already expired?)");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}

		if (restKb.getLease() == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage(
					"Knowledge base found, but its lease could not be found. Knowledge bases without a lease do not have to be renewed.");
			return Response.status(Status.NOT_FOUND).entity(response).build();
		}

		restKb.renewLease();

		return Response.ok(restKb.getLease()).build();
	}
}
