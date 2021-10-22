package eu.knowledge.engine.rest.api.impl;

import java.net.URI;
import java.net.URISyntaxException;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.api.SmartConnectorLeaseApiService;

public class SmartConnectorLeaseApiServiceImpl extends SmartConnectorLeaseApiService {

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@Override
	public Response scLeaseRenewPut(@NotNull String knowledgeBaseId, SecurityContext securityContext)
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
			return Response.status(404).entity("Knowledge base not found. (Has its lease already expired?)").build();
		}
		
		if (restKb.getLease() == null) {
			return Response.status(404).entity("Knowledge base found, but its lease could not be found. Knowledge bases without a lease do not have to be renewed.").build();
		}

		restKb.renewLease();

		return Response.ok(restKb.getLease()).build();
	}
}
