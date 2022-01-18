package eu.knowledge.engine.rest.api.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.model.KnowledgeInteractionWithId;
import io.swagger.annotations.ApiParam;

@Path("/sc")
public class ReactiveApiServiceImpl {

	private static final String REACT_KNOWLEDGE_INTERACTION = "ReactKnowledgeInteraction";

	private static final String ANSWER_KNOWLEDGE_INTERACTION = "AnswerKnowledgeInteraction";

	public static final int LONGPOLL_TIMEOUT = 29;

	private static final Logger LOG = LoggerFactory.getLogger(ReactiveApiServiceImpl.class);

	private RestKnowledgeBaseManager manager = RestKnowledgeBaseManager.newInstance();

	@GET
	@Path("/handle")
	@Consumes({ "application/json; charset=UTF-8" })
	@Produces({ "application/json; charset=UTF-8", "text/plain; charset=UTF-8" })
	public void scHandleGet(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") @NotNull String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException, IOException {

		LOG.info("scHandleGet() called by KB: {}", knowledgeBaseId);
		asyncResponse.setTimeout(LONGPOLL_TIMEOUT, TimeUnit.SECONDS);
		try {
			// validate kb id
			new URI(knowledgeBaseId);

			if (this.manager.hasKB(knowledgeBaseId)) {

				RestKnowledgeBase kb = manager.getKB(knowledgeBaseId);
				kb.resetInactivityTimeout();

				// handler that returns status code 202
				TimeoutHandler handler = new TimeoutHandler() {
					@Override
					public void handleTimeout(AsyncResponse ar) {
						LOG.debug("Sending 202 to {}", kb.getKnowledgeBaseId());
						ResponseBuilder aBuilder;

						aBuilder = Response.status(Status.ACCEPTED);

						String message = String.format("This is a heartbeat message that the server is still alive, please renew your long polling request within %d seconds.", RestKnowledgeBase.INACTIVITY_TIMEOUT_SECONDS);
						if (!kbHasAnswerOrReactKnowledgeInteraction(kb)) {
							message += " Knowledge Base '" + kb.getKnowledgeBaseId().toString()
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
					asyncResponse.resume(Response.status(Status.CONFLICT)
							.entity("Only one connection per Knowledge-Base-Id is allowed and we already have one.")
							.build());
					return;
				}
			} else {
				if (this.manager.hasSuspendedKB(knowledgeBaseId)) {
					this.manager.removeSuspendedKB(knowledgeBaseId);
					asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(
							"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
						.build());
					return;
				} else {
					asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(
							"A Knowledge Base for the given Knowledge-Base-Id cannot be found.")
						.build());
					return;
				}
			}
		} catch (URISyntaxException e) {
			asyncResponse.resume(Response.status(Status.BAD_REQUEST)
					.entity("Smart Connector not found, because its ID must be a valid URI.").build());
			return;
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
	@Consumes({ "application/json; charset=UTF-8" })
	@Produces({ "application/json; charset=UTF-8", "text/plain; charset=UTF-8" })
	public Response scHandlePost(
			@ApiParam(value = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") @NotNull String knowledgeBaseId,
			@ApiParam(value = "The Post Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") @NotNull String knowledgeInteractionId,
			@ApiParam(value = "") @Valid @NotNull eu.knowledge.engine.rest.model.HandleResponse responseBody,
			@Context SecurityContext securityContext) throws NotFoundException {

		LOG.info("scHandlePost() called with {}, {}, {}", knowledgeBaseId, knowledgeInteractionId, responseBody);

		if (knowledgeBaseId == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Smart Connector not found, because its ID must be a valid URI and not " + knowledgeBaseId)
					.build();
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Smart Connector not found, because its ID must be a valid URI and not " + knowledgeBaseId)
					.build();
		}

		if (manager.hasKB(knowledgeBaseId)) {
			// knowledgebase exists
			RestKnowledgeBase kb = manager.getKB(knowledgeBaseId);

			if (kb.hasKnowledgeInteraction(knowledgeInteractionId)) {
				// knowledge interaction exists

				if (kb.hasHandleRequestId(responseBody.getHandleRequestId())) {

					try {
						kb.finishHandleRequest(knowledgeInteractionId, responseBody);
					} catch (IllegalArgumentException e) {
						return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
					}

					return Response.ok().build();
				} else {
					return Response.status(Status.NOT_FOUND).entity("Handle request id " + responseBody.getHandleRequestId()
							+ " not found. Are you sure it is still being processed?").build();
				}
			} else {
				return Response.status(Status.BAD_REQUEST)
						.entity("Knowledge Interaction not found, because its ID must match an existing KI: "
								+ knowledgeInteractionId)
						.build();
			}
		} else {
			if (manager.hasSuspendedKB(knowledgeBaseId)) {
				manager.removeSuspendedKB(knowledgeBaseId);
				return Response.status(Status.NOT_FOUND).entity(
						"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.")
					.build();
			} else {
				return Response.status(Status.NOT_FOUND).entity("A Knowledge Base for the given Knowledge-Base-Id cannot be found.")
					.build();
			}
		}
	}

}
