package eu.knowledge.engine.rest.api.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.model.KnowledgeInteractionWithId;
import eu.knowledge.engine.rest.model.ResponseMessage;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.container.TimeoutHandler;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

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
			@Parameter(description = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") @NotNull String knowledgeBaseId,
			@Suspended final AsyncResponse asyncResponse, @Context SecurityContext securityContext)
			throws NotFoundException, IOException {

		LOG.debug("scHandleGet() called by KB: {}", knowledgeBaseId);
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

						String message = String.format(
								"This is a heartbeat message that the server is still alive, please renew your long polling request within %d seconds.",
								RestKnowledgeBase.INACTIVITY_TIMEOUT_SECONDS);
						if (!kbHasAnswerOrReactKnowledgeInteraction(kb)) {
							message += " Knowledge Base '" + kb.getKnowledgeBaseId().toString()
									+ "' does not have any Answer or React Knowledge Interaction registered. Only those two types of Knowledge Interactions require the usage of this long polling method.";
							var response = new ResponseMessage();
							response.setMessageType("info");
							response.setMessage(message);
							aBuilder.entity(response);
						}
						kb.resetAsyncResponse();
						ar.resume(aBuilder.build());
					}
				};

				asyncResponse.setTimeoutHandler(handler);

				if (!kb.hasAsyncResponse()) {
					kb.waitForHandleRequest(asyncResponse);
				} else {
					var response = new ResponseMessage();
					response.setMessageType("error");
					response.setMessage(
							"Only one connection per Knowledge-Base-Id is allowed and we already have one.");
					asyncResponse.resume(Response.status(Status.CONFLICT).entity(response).build());
					return;
				}
			} else {
				if (this.manager.hasSuspendedKB(knowledgeBaseId)) {
					this.manager.removeSuspendedKB(knowledgeBaseId);
					var response = new ResponseMessage();
					response.setMessageType("error");
					response.setMessage(
							"This knowledge base has been suspended due to inactivity. Please reregister the knowledge base and its knowledge interactions.");
					asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(response).build());
					return;
				} else {
					var response = new ResponseMessage();
					response.setMessageType("error");
					response.setMessage("A Knowledge Base for the given Knowledge-Base-Id cannot be found.");
					asyncResponse.resume(Response.status(Status.NOT_FOUND).entity(response).build());
					return;
				}
			}
		} catch (URISyntaxException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage("Smart Connector not found, because its ID must be a valid URI.");
			asyncResponse.resume(Response.status(Status.BAD_REQUEST).entity(response).build());
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
			@Parameter(description = "The Knowledge Base Id for which to execute the ask.", required = true) @HeaderParam("Knowledge-Base-Id") @NotNull String knowledgeBaseId,
			@Parameter(description = "The Post Knowledge Interaction Id to execute.", required = true) @HeaderParam("Knowledge-Interaction-Id") @NotNull String knowledgeInteractionId,
			@Parameter(description = "") @Valid @NotNull eu.knowledge.engine.rest.model.HandleResponse responseBody,
			@Context SecurityContext securityContext) throws NotFoundException {

		LOG.info("scHandlePost() called with {}, {}", knowledgeBaseId, knowledgeInteractionId);
		LOG.debug("scHandlePost() received this response body: {}", responseBody);

		if (knowledgeBaseId == null) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage(
					"Smart Connector not found, because its ID must be a valid URI and not " + knowledgeBaseId);
			return Response.status(Status.BAD_REQUEST).entity(response).build();
		}

		try {
			new URI(knowledgeBaseId);
		} catch (URISyntaxException e) {
			var response = new ResponseMessage();
			response.setMessageType("error");
			response.setMessage(
					"Smart Connector not found, because its ID must be a valid URI and not " + knowledgeBaseId);
			return Response.status(Status.BAD_REQUEST).entity(response).build();
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
						var response = new ResponseMessage();
						response.setMessageType("error");
						response.setMessage(e.getMessage());
						return Response.status(Status.BAD_REQUEST).entity(response).build();
					}

					return Response.ok().build();
				} else {
					var response = new ResponseMessage();
					response.setMessageType("error");
					response.setMessage("Handle request id " + responseBody.getHandleRequestId()
							+ " not found. Are you sure it is still being processed?");
					return Response.status(Status.NOT_FOUND).entity(response).build();
				}
			} else {
				var response = new ResponseMessage();
				response.setMessageType("error");
				response.setMessage("Knowledge Interaction not found, because its ID must match an existing KI: "
						+ knowledgeInteractionId);
				return Response.status(Status.BAD_REQUEST).entity(response).build();
			}
		} else {
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
				response.setMessage("A Knowledge Base for the given Knowledge-Base-Id cannot be found.");
				return Response.status(Status.NOT_FOUND).entity(response).build();
			}
		}
	}

}
