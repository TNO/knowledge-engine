package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.MessagingApiService;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.ErrorMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.ReactMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

/**
 * This class is responsible for receiving messages from all remote Smart
 * Connectors. There should be only one instance of this class. This is the
 * HTTPS server.
 */
public class RemoteMessageReceiver extends MessagingApiService {

	public static Logger LOG = LoggerFactory.getLogger(RemoteMessageReceiver.class);

	private Response handleMessage(KnowledgeMessage message) {
		try {
			LOG.trace("Received {} {} from KnowledgeDirectory for KnowledgeBase {} from remote SmartConnector",
					message.getClass().getSimpleName(), message.getMessageId(), message.getToKnowledgeBase());
			KeRuntime.getMessageDispatcher().deliver(message);
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
	public Response messagingAskmessagePost(AskMessage askMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(MessageConverter.fromJson(askMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingAnswermessagePost(AnswerMessage answerMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(MessageConverter.fromJson(answerMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			// Could not parse message
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingPostmessagePost(PostMessage postMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(MessageConverter.fromJson(postMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingReactmessagePost(ReactMessage reactMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(MessageConverter.fromJson(reactMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

	@Override
	public Response messagingErrormessagePost(ErrorMessage errorMessage, SecurityContext securityContext)
			throws NotFoundException {
		try {
			return handleMessage(MessageConverter.fromJson(errorMessage));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return createErrorResponse(e);
		}
	}

}
