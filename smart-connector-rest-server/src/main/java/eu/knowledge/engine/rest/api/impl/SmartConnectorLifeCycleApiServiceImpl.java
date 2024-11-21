package eu.knowledge.engine.rest.api.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIProvider;
import org.apache.jena.irix.IRIProviderJenaIRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.model.ResponseMessage;
import eu.knowledge.engine.rest.model.SmartConnector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

@Path("/sc")
public class SmartConnectorLifeCycleApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(SmartConnectorLifeCycleApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	private final IRIProvider iriProvider = new IRIProviderJenaIRI();

	@GET
	@Produces({ "application/json; charset=UTF-8" })
	@Operation(summary = "Either get all available Smart Connectors or a specific one if the Knowledge-Base-Id is provided.", description = "", tags = {
			"smart connector life cycle" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "A list of Smart Connectors. It will have only a single element if the Knowledge-Base-Id was provided.", content = @Content(schema = @Schema(implementation = SmartConnector.class))),
			@ApiResponse(responseCode = "404", description = "If there is no Smart Connector for the given Knowledge-Base-Id.", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "500", description = "If a problem occurred.", content = @Content(schema = @Schema(implementation = String.class))) })

	public void scGet(
			@Parameter(description = "The knowledge base id who's Smart Connector information you would like to have.") @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException {
		if (knowledgeBaseId == null) {
			asyncResponse.resume(Response.ok().entity(convertToModel(this.manager.getKBs())).build());
			return;
		} else {
			try {
				new URI(knowledgeBaseId);
			} catch (URISyntaxException e) {
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Knowledge base not found, because its ID must be a valid URI.");
				asyncResponse.resume(Response.status(400).entity(response).build());
				return;
			}
			if (this.manager.hasKB(knowledgeBaseId)) {
				Set<RestKnowledgeBase> connectors = new HashSet<>();
				connectors.add(this.manager.getKB(knowledgeBaseId));
				asyncResponse.resume(Response.ok().entity(convertToModel(connectors)).build());
				return;
			} else {
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Knowledge base not found.");
				asyncResponse.resume(Response.status(404).entity(response).build());
				return;
			}
		}
	}

	@POST
	@Consumes({ "application/json; charset=UTF-8" })
	@Produces({ "application/json; charset=UTF-8" })
	@Operation(summary = "Create a new Smart Connector for the given Knowledge Base.", description = "", tags = {
			"smart connector life cycle", })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "If the Smart Connector for the given Knowledge Base is successfully created.", content = @Content(schema = @Schema(implementation = Void.class))),
			@ApiResponse(responseCode = "400", description = "If the creation of the Smart Connector for the given Knowledge Base failed.", content = @Content(schema = @Schema(implementation = String.class))) })
	public void scPost(@Parameter(description = "", required = true) @NotNull @Valid SmartConnector smartConnector,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException {
		if (smartConnector.getKnowledgeBaseId().isEmpty()) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge Base ID must be a non-empty URI.");
			asyncResponse.resume(Response.status(400).entity(response).build());
			return;
		}

		try {
			new URL(smartConnector.getKnowledgeBaseId()).toURI();
		} catch (MalformedURLException | URISyntaxException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge base ID must be a valid URI.");
			asyncResponse.resume(Response.status(400).entity(response).build());
			return;
		}

		URI kbId;
		try {
			// Additional check to verify that it is a valid IRI according to Jena.
			// (java.net.URI is not strict enough.)
			iriProvider.check(smartConnector.getKnowledgeBaseId());

			kbId = new URI(smartConnector.getKnowledgeBaseId());
		} catch (URISyntaxException | IRIException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge base ID must be a valid IRI.");
			asyncResponse.resume(Response.status(400).entity(response).build());
			return;
		}

		final String kbDescription = smartConnector.getKnowledgeBaseDescription();
		final String kbName = smartConnector.getKnowledgeBaseName();

		if (this.manager.hasKB(smartConnector.getKnowledgeBaseId())) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("That knowledge base ID is already in use.");
			asyncResponse.resume(Response.status(400).entity(response).build());
			return;
		}

		final boolean reasonerEnabled = smartConnector.getReasonerEnabled() == null ? false
				: smartConnector.getReasonerEnabled();
				
		LOG.info("Creating smart connector with ID {} and reasoner enabled '{}'.", kbId, reasonerEnabled);
		
		// Tell the manager to create a KB, store it, and have it set up a SC etc.
		this.manager.createKB(new SmartConnector().knowledgeBaseId(kbId.toString()).knowledgeBaseName(kbName)
				.knowledgeBaseDescription(kbDescription).leaseRenewalTime(smartConnector.getLeaseRenewalTime())
				.reasonerEnabled(reasonerEnabled)).thenRun(() -> {
					LOG.info("Returning response for smart connector with ID {}", kbId);
					asyncResponse.resume(Response.ok().build());
				});

		return;
	}

	@DELETE
	@Produces({ "application/json; charset=UTF-8" })
	@Operation(summary = "Delete the Smart Connector belonging to the given Knowledge Base", description = "", tags = {
			"smart connector life cycle", })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "If the Smart Connector for the given Knowledge Base is successfully deleted.", content = @Content(schema = @Schema(implementation = Void.class))),
			@ApiResponse(responseCode = "404", description = "If there is no Smart Connector for the given Knowledge-Base-Id.", content = @Content(schema = @Schema(implementation = String.class))) })
	public void scDelete(
			@Parameter(description = "The knowledge base id who's smart connector should be deleted.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException {
		LOG.info("scDelete called: {}", knowledgeBaseId);

		if (knowledgeBaseId == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge-Base-Id header should not be null.");
			asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity(response).build());
			return;
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Knowledge base not found, because its ID must be a valid URI.");
			asyncResponse.resume(Response.status(400).entity(response).build());
			return;
		}

		if (manager.hasKB(knowledgeBaseId)) {
			if (manager.deleteKB(knowledgeBaseId)) {
				LOG.info("Deleted smart connector with ID {}.", knowledgeBaseId);
				asyncResponse.resume(Response.ok().build());
				return;
			} else {
				LOG.warn(
						"Deletion failed of smart connector with ID {} because it was already stopping. Returning 404.",
						knowledgeBaseId);
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Deletion of knowledge base failed, because it was already being deleted.");
				asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(response).build());
				return;
			}
		} else {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Deletion of knowledge base failed, because it could not be found.");
			asyncResponse.resume(Response.status(404).entity(response).build());
			return;
		}
	}

	private eu.knowledge.engine.rest.model.SmartConnector[] convertToModel(Set<RestKnowledgeBase> kbs) {
		return kbs.stream().map((restKb) -> {
			return new eu.knowledge.engine.rest.model.SmartConnector()
					.knowledgeBaseId(restKb.getKnowledgeBaseId().toString())
					.knowledgeBaseName(restKb.getKnowledgeBaseName())
					.knowledgeBaseDescription(restKb.getKnowledgeBaseDescription())
					.leaseRenewalTime(restKb.getLeaseRenewalTime()).reasonerEnabled(restKb.getReasonerEnabled());
		}).toArray(eu.knowledge.engine.rest.model.SmartConnector[]::new);
	}
}
