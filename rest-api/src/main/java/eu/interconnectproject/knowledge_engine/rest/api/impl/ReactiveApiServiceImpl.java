package eu.interconnectproject.knowledge_engine.rest.api.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.rest.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.rest.model.KnowledgeInteractionWithId;
import io.swagger.annotations.ApiParam;

@Path("/sc")
public class ReactiveApiServiceImpl {

	private static final String REACT_KNOWLEDGE_INTERACTION = "ReactKnowledgeInteraction";

	private static final String ANSWER_KNOWLEDGE_INTERACTION = "AnswerKnowledgeInteraction";

	private static final int TIMEOUT = 29;

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@GET
	@Path("/handle")
	@Consumes({ "application/json" })
	@Produces({ "application/json", "text/plain" })
	public void scHandleGet(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException, IOException {

		LOG.info("scHandleGet() called!");
		asyncResponse.setTimeout(TIMEOUT, TimeUnit.SECONDS);
		try {
			// validate kb id
			new URI(knowledgeBaseId);

			if (manager.hasKB(knowledgeBaseId)) {

				RestKnowledgeBase kb = manager.getKB(knowledgeBaseId);

				// handler that returns status code 202
				TimeoutHandler handler = new TimeoutHandler() {
					@Override
					public void handleTimeout(AsyncResponse ar) {
						LOG.info("handleTimeout called!");
						ResponseBuilder aBuilder;

						aBuilder = Response.status(202);

						String message = "This is a heartbeat message that the server is still alive, please renew your long polling request. ";
						if (!kbHasAnswerOrReactKnowledgeInteraction(kb)) {
							message += "Knowledge Base '" + kb.getKnowledgeBaseId().toString()
									+ "' does not have any Answer or React Knowledge Interaction registered. Only those two types of Knowledge Interactions require the usage of this long polling method.";
							aBuilder.entity(message);
						}
						kb.resetAsyncResponse();
						ar.resume(aBuilder.build());

					}
				};

				asyncResponse.setTimeoutHandler(handler);

				if (!kb.hasAsyncResponse()) {
					kb.waitForHandleRequest(asyncResponse);
				} else {
					asyncResponse.resume(Response.status(400)
							.entity("Only one connection per Knowledge-Base-Id is allowed and we already have one.")
							.build());
				}
			} else {
				asyncResponse.resume(Response.status(404).entity(
						"A Knowledge Base for the given Knowledge-Base-Id cannot be found.")
						.build());
			}
		} catch (URISyntaxException e) {
			asyncResponse.resume(Response.status(400)
					.entity("Smart Connector not found, because its ID must be a valid URI.").build());
		}
	}

	private boolean kbHasAnswerOrReactKnowledgeInteraction(RestKnowledgeBase kb) {

		boolean hasAnswerOrReact = false;
		for (KnowledgeInteractionWithId kis : kb.getKnowledgeInteractions()) {
			if (kis.getKnowledgeInteractionType().equals(ANSWER_KNOWLEDGE_INTERACTION)
					|| kis.getKnowledgeInteractionType().equals(REACT_KNOWLEDGE_INTERACTION)) {

				hasAnswerOrReact = true;
				break;
			}
		}

		return hasAnswerOrReact;
	}

	@POST
	@Path("/handle")
	@Consumes({ "application/json" })
	@Produces({ "application/json", "text/plain" })
	public Response scHandlePost(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") String knowledgeBaseId,
			@ApiParam(value = "The Post Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") String knowledgeInteractionId,
			@ApiParam(value = "") @Valid eu.interconnectproject.knowledge_engine.rest.model.HandleRequest requestBody,
			@Context SecurityContext securityContext) throws NotFoundException {

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

					try {
						kb.finishHandleRequest(knowledgeInteractionId, requestBody);
					} catch (IllegalArgumentException e) {
						return Response.status(400).entity(e.getMessage()).build();
					}

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
