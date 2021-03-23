package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.rest.model.InlineObject1;
import io.swagger.annotations.ApiParam;

@Path("/sc")
public class ReactiveApiServiceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@GET
	@Path("/handle")
	@Consumes({ "application/json" })
	@Produces({ "application/json", "text/plain" })
	public void scHandleGet(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext,
			@Context HttpServletRequest servletRequest) throws NotFoundException, IOException {

//		asyncResponse.cancel();

		LOG.info("scHandleGet() called!");
		assert servletRequest.isAsyncStarted();
		final AsyncContext asyncContext = servletRequest.getAsyncContext();

		ServletResponse r = asyncContext.getResponse();
		assert r instanceof HttpServletResponse;
		HttpServletResponse httpResponse = (HttpServletResponse) r;

		// TODO can we wrap the servletresponse in a JAX-RS response?
		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			httpResponse.sendError(400, "Smart Connector not found, because its ID must be a valid URI.");
		}

		if (manager.hasKB(knowledgeBaseId)) {

			RestKnowledgeBase kb = manager.getKB(knowledgeBaseId);

			if (!kb.hasAsyncContext()) {
				kb.handleRequest(asyncContext);
			} else {
				httpResponse.sendError(500,
						"Only one connection per Knowledge-Base-Id is allowed and we already have one.");
				asyncContext.complete();
			}
		} else {
			httpResponse.sendError(404,
					"If a Knowledge Interaction for the given Knowledge-Base-Id and Knowledge-Interaction-Id cannot be found.");
		}
	}

	@POST
	@Path("/handle")
	@Consumes({ "application/json" })
	@Produces({ "application/json", "text/plain" })
	public Response scHandlePost(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@ApiParam(value = "The Post Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") String knowledgeInteractionId,
			@ApiParam(value = "") @Valid InlineObject1 requestBody, @Context SecurityContext securityContext)
			throws NotFoundException {

		LOG.info("scHandlePost() called with {}, {}, {}", knowledgeBaseId, knowledgeInteractionId, requestBody);

		if (knowledgeBaseId == null) {
			return Response.status(400)
					.entity("Smart Connector not found, because its ID must be a valid URI and not " + knowledgeBaseId)
					.build();
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			return Response.status(400)
					.entity("Smart Connector not found, because its ID must be a valid URI and not " + knowledgeBaseId)
					.build();
		}

		if (manager.hasKB(knowledgeBaseId)) {
			// knowledgebase exists
			RestKnowledgeBase kb = manager.getKB(knowledgeBaseId);

			if (kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
				// knowledge interaction exists

				if (kb.hasHandleRequestId(requestBody.getHandleRequestId())) {

					kb.finishHandleRequest(knowledgeInteractionId, requestBody);

					return Response.ok().build();
				} else {
					return Response.status(404).entity("Handle request id " + requestBody.getHandleRequestId()
							+ " not found. Are you sure it is still being processed?").build();
				}

			} else {
				return Response.status(400)
						.entity("Knowledge Interaction not found, because its ID must match an existing KI: "
								+ knowledgeInteractionId)
						.build();
			}

		} else {
			return Response.status(400).entity(
					"If a Knowledge Interaction for the given Knowledge-Base-Id and Knowledge-Interaction-Id cannot be found.")
					.build();
		}
	}

}
