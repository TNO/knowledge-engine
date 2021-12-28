package eu.knowledge.engine.rest.api.impl;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.model.SmartConnector;
import eu.knowledge.engine.admin.AdminUI;
import io.swagger.annotations.ApiParam;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIProvider;
import org.apache.jena.irix.IRIProviderJenaIRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Path("/admin")
public class AdminApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(AdminApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	private final IRIProvider iriProvider = new IRIProviderJenaIRI();

	//private static AdminUI admin;
	private AdminUI admin = AdminUI.newInstance();

	//todo: Add TKE runtimes + Smart connectors per runtime in JSON response
	//todo: add active=true|false (show historical SCs, even after lease is expired. Missing or lost SCs can also be valuable information.)
	//todo: add registered knowledge interactions
	//todo: add log with timestamps of all instances of knowledge interaction (in client/GUI we can show time since last interaction)
	//todo: Select smart connector or knowledge interactions based on Knowledge-Base-Id (re-use get route in SmartConnectorLifeCycleApiServiceImpl.java)
	//todo: make route which only gets updated info since <timestamp> (for longpolling)
	//todo: test if suitable for long polling

	@GET
	@Path("/overview")
	@Produces({ "application/json; charset=UTF-8", "text/plain; charset=UTF-8" })
	@io.swagger.annotations.ApiOperation(value = "Get all available Knowledge Engine Runtimes and Smart Connectors in the network.", notes = "", response = SmartConnector.class, responseContainer = "List", tags={ "smart connector life cycle", })
	@io.swagger.annotations.ApiResponses(value = {
		@io.swagger.annotations.ApiResponse(code = 200, message = "A list of Knowledge Engine Runtimes and, for each, its Smart Connectors.", response = SmartConnector.class, responseContainer = "List"),
		@io.swagger.annotations.ApiResponse(code = 404, message = "If there are no Knowledge Engine Runtimes for the given Knowledge-Engine-Id.", response = String.class),
		@io.swagger.annotations.ApiResponse(code = 500, message = "If a problem occurred.", response = String.class)
	})
	public void scGet(
		@ApiParam(value = "The knowledge base id who's Smart Connector information you would like to have." )@HeaderParam("Knowledge-Engine-Id") String knowledgeEngineId,
		@Suspended final AsyncResponse asyncResponse,
		@Context SecurityContext securityContext
	) throws NotFoundException {
		if (knowledgeEngineId == null) {
			asyncResponse.resume(Response.ok().entity(convertToModel(this.manager.getKBs())).build());
			return;
		} else {
			try {
				new URI(knowledgeEngineId);
			} catch (URISyntaxException e) {
				asyncResponse.resume(Response.status(400).entity("Knowledge engine runtime not found, because its ID must be a valid URI.")
						.build());
				return;
			}
			if (this.manager.hasKB(knowledgeEngineId)) {
				Set<RestKnowledgeBase> connectors = new HashSet<>();
				connectors.add(this.manager.getKB(knowledgeEngineId));
				asyncResponse.resume(Response.ok().entity(convertToModel(connectors)).build());
				return;
			} else {
				asyncResponse.resume(Response.status(404).entity("Knowledge base not found.").build());
				return;
			}
		}
	}

	private SmartConnector[] convertToModel(
			Set<RestKnowledgeBase> kbs) {
		return kbs.stream().map((restKb) -> {
			return new SmartConnector()
					.knowledgeBaseId(restKb.getKnowledgeBaseId().toString())
					.knowledgeBaseName(restKb.getKnowledgeBaseName())
					.knowledgeBaseDescription(restKb.getKnowledgeBaseDescription())
					.leaseRenewalTime(restKb.getLeaseRenewalTime());
		}).toArray(SmartConnector[]::new);
	}
}
