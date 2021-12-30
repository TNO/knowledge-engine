package eu.knowledge.engine.admin.api.impl;

import eu.knowledge.engine.admin.AdminUI;
import eu.knowledge.engine.admin.AdminUI.*;
import eu.knowledge.engine.admin.api.RestServer;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/admin")
public class AdminApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(AdminApiServiceImpl.class);

	private AdminUI admin;

	//todo: Add TKE runtimes + Smart connectors per runtime in JSON response
	//todo: add active=true|false (show historical SCs, even after lease is expired. Missing or lost SCs can also be valuable information.)
	//todo: add registered knowledge interactions
	//todo: add log with timestamps of all instances of knowledge interaction (in client/GUI we can show time since last interaction)
	//todo: Select smart connector or knowledge interactions based on Knowledge-Base-Id (re-use get route in SmartConnectorLifeCycleApiServiceImpl.java)
	//todo: make route which only gets updated info since <timestamp> (for longpolling)
	//todo: test if suitable for long polling
	//@io.swagger.annotations.ApiOperation(value = "Get all available Knowledge Engine Runtimes and Smart Connectors in the network.", notes = "", response = KnowledgeEngineRuntimeConnectionDetails.class, responseContainer = "List", tags = {"smart connector life cycle",})
	//@Path("{TKE_ID}/kbs/overview")
	//TODO: create response = model.class return type of routes

	@GET
	@Path("/kb/overview")
	@Produces({"application/json; charset=UTF-8", "text/plain; charset=UTF-8"})
	@io.swagger.annotations.ApiOperation(value = "Get all available knowledge bases and their knowledge interactions in the network.", notes = "", response = String.class, responseContainer = "List", tags = {"admin UI API",})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "A list of knowledge bases and, for each, its knowledge interactions.", response = String.class, responseContainer = "List"),
			@io.swagger.annotations.ApiResponse(code = 404, message = "If there are no knowledge bases in the given Knowledge-Engine-Id.", response = String.class),
			@io.swagger.annotations.ApiResponse(code = 500, message = "If a problem occurred.", response = String.class)
	})
	public void getKBOverview(
			@ApiParam(value = "The knowledge Engine Runtime who's information (knowledge bases, knowledge interactions) you would like to have.") @HeaderParam("Knowledge-Engine-Id") String knowledgeEngineId,
			@Suspended final AsyncResponse asyncResponse,
			@Context SecurityContext securityContext
	) throws NotFoundException {
		admin = AdminUI.newInstance(); //or start when init/start API route is called?
		if (knowledgeEngineId == null) {
			//asyncResponse.resume(Response.ok().entity(convertToModel(this.manager.getKBs())).build());
			//asyncResponse.resume(Response.ok().entity(this.admin).build());
			asyncResponse.resume(Response.ok().entity(this.admin.getModel()).build());
			return;
		} else {
			return;
		}
	}
}
/*
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

}*/
