package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ErrorMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.RFC3339DateFormat;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.KnowledgeEngineRuntimeDetails;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

/**
 * This class is responsible for sending messages to a single remote Knowledge
 * Engine Runtime (KER) and keeping the {@link KnowledgeEngineRuntimeDetails}
 * up-to-date (both ways).
 */
public class RemoteKerConnection {

	public static final Logger LOG = LoggerFactory.getLogger(RemoteKerConnection.class);

	private final KnowledgeEngineRuntimeConnectionDetails kerConnectionDetails;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	private KnowledgeEngineRuntimeDetails kerDetails;

	public RemoteKerConnection(KnowledgeEngineRuntimeConnectionDetails kerConnectionDetails) {
		this.kerConnectionDetails = kerConnectionDetails;

		httpClient = HttpClient.newBuilder().build();

		objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).findAndRegisterModules()
				.setDateFormat(new RFC3339DateFormat());
	}

	/**
	 * Contact the Knowledge Engine Runtime to retrieve the latest
	 * {@link KnowledgeEngineRuntimeDetails}
	 */
	private void updateKerDataFromPeer() {
		try {
			HttpRequest request = HttpRequest
					.newBuilder(new URI(DistributedMessageDispatcher.PEER_PROTOCOL + "://" + kerConnectionDetails.getHostname() + ":"
							+ kerConnectionDetails.getPort() + "/runtimedetails"))
					.header("Content-Type", "application/json").GET().build();

			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				LOG.trace(
						"Successfully received runtimedetails from " + kerConnectionDetails.getHostname() + ":" + kerConnectionDetails.getPort());
				KnowledgeEngineRuntimeDetails runtimeDetails = objectMapper.readValue(response.body(),
						KnowledgeEngineRuntimeDetails.class);
				// TODO validate
				this.kerDetails = runtimeDetails;
			} else {
				LOG.warn("Failed to received runtimedetails from " + kerConnectionDetails.getHostname() + ":" + kerConnectionDetails.getPort()
						+ ", got status code " + response.statusCode());
			}
		} catch (IOException | URISyntaxException | InterruptedException e) {
			LOG.warn("Failed to received runtimedetails from " + kerConnectionDetails.getHostname() + ":" + kerConnectionDetails.getPort(), e);
		}
	}

	public KnowledgeEngineRuntimeDetails getKerDetails() {
		if (kerDetails == null) {
			updateKerDataFromPeer();
		}
		return kerDetails;
	}

	public void setKerDetails(KnowledgeEngineRuntimeDetails kerDetails) {
		this.kerDetails = kerDetails;
		// TODO implement checks?
	}

	public void start() {
		this.updateKerDataFromPeer();
	}

	public void stop() {
		// TODO call the remote DELETE so the other side knows we're leaving
	}

	public void sendToRemoteSmartConnector(KnowledgeMessage message) throws IOException {
		assert getKerDetails().getSmartConnectorIds().contains(message.getToKnowledgeBase().toString());

		try {
			String jsonMessage = objectMapper.writeValueAsString(MessageConverter.toJson(message));
			HttpRequest request = HttpRequest
					.newBuilder(new URI(DistributedMessageDispatcher.PEER_PROTOCOL + "://" + kerConnectionDetails.getHostname() + ":"
							+ kerConnectionDetails.getPort() + getPathForMessageType(message)))
					.header("Content-Type", "application/json").POST(BodyPublishers.ofString(jsonMessage)).build();

			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				LOG.trace("Successfully sent message {} to {}:{}", message.getMessageId(), kerConnectionDetails.getHostname(),
						kerConnectionDetails.getPort());
			} else {
				LOG.warn("Failed to send message {} to {}:{}, got response {}: {}", message.getMessageId(),
						kerConnectionDetails.getHostname(), kerConnectionDetails.getPort(), response.statusCode(), response.body());
				throw new IOException("Message not accepted by remote host, status code " + response.statusCode()
						+ ", body " + response.body());
			}
		} catch (JsonProcessingException | URISyntaxException | InterruptedException e) {
			throw new IOException("Could not send message to remote Smart Connector", e);
		}
	}

	private String getPathForMessageType(KnowledgeMessage message) {
		if (message instanceof AskMessage) {
			return "/messaging/askmessage";
		} else if (message instanceof AnswerMessage) {
			return "/messaging/answermessage";
		} else if (message instanceof PostMessage) {
			return "/messaging/postmessage";
		} else if (message instanceof ReactMessage) {
			return "/messaging/reactmessage";
		} else if (message instanceof ErrorMessage) {
			return "/messaging/errormessage";
		} else {
			return null;
		}
	}

}
