package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.MessagingApiService;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.NotFoundException;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.AnswerMessage;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.AskMessage;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.ErrorMessage;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.PostMessage;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.ReactMessage;

/**
 * This class is responsible for receiving messages from all remote Smart
 * Connectors. There should be only one instance of this class. This is the
 * HTTPS server.
 */
public class RemoteMessageReceiver extends MessagingApiService {

	public static Logger LOG = LoggerFactory.getLogger(RemoteMessageReceiver.class);

	private final MessageDispatcher messageDispatcher;

	public RemoteMessageReceiver(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	private Response handleMessage(String authorizationToken, KnowledgeMessage message) {
		LOG.trace("Received {} {} from KnowledgeDirectory for KnowledgeBase {} from remote SmartConnector",
				message.getClass().getSimpleName(), message.getMessageId(), message.getToKnowledgeBase());
		try {
			messageDispatcher.deliverToLocalSmartConnector(message);
			return Response.status(202).build();
		} catch (IOException e) {
			// Was not able to deliver message to the SmartConnector
			return createErrorResponse(e);
		}
	}

	private Response createErrorResponse(Exception e) {
		LOG.warn("Error while handling incoming message", e);
		return Response.status(400).header("Content-Type", "text/plain").entity("Error: " + e.getMessage()).build();
	}

	@Override
	public Response messagingAskmessagePost(String authorizationToken, AskMessage askMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(authorizationToken, MessageConverter.fromJson(askMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingAnswermessagePost(String authorizationToken, AnswerMessage answerMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(authorizationToken, MessageConverter.fromJson(answerMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			// Could not parse message
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingPostmessagePost(String authorizationToken, PostMessage postMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(authorizationToken, MessageConverter.fromJson(postMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingReactmessagePost(String authorizationToken, ReactMessage reactMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(authorizationToken, MessageConverter.fromJson(reactMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingErrormessagePost(String authorizationToken, ErrorMessage errorMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(authorizationToken, MessageConverter.fromJson(errorMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

}
