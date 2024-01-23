package eu.knowledge.engine.smartconnector.runtime.messaging;

import static eu.knowledge.engine.smartconnector.runtime.messaging.Utils.stripUserInfoFromURI;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.ErrorMessage;
import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.RFC3339DateFormat;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.KnowledgeEngineRuntimeDetails;
import eu.knowledge.engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

/**
 * This class is responsible for sending messages to a single remote Knowledge
 * Engine Runtime (KER) and keeping the {@link KnowledgeEngineRuntimeDetails}
 * up-to-date (both ways).
 */
public class RemoteKerConnection {

	/**
	 * A maximum amount of time to wait for othe HTTP REST call to fail/succeed.
	 */
	private static final int HTTP_TIMEOUT = 30;

	public static final Logger LOG = LoggerFactory.getLogger(RemoteKerConnection.class);

	private final KnowledgeEngineRuntimeConnectionDetails remoteKerConnectionDetails;
	private final URI remoteKerUri;
	private KnowledgeEngineRuntimeDetails remoteKerDetails;
	private final MessageDispatcher dispatcher;

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	private LocalDateTime tryAgainAfter = null;
	private int errorCounter = 0;

	public RemoteKerConnection(MessageDispatcher dispatcher,
			KnowledgeEngineRuntimeConnectionDetails kerConnectionDetails) {
		this.dispatcher = dispatcher;
		this.remoteKerConnectionDetails = kerConnectionDetails;

		var builder = HttpClient.newBuilder();

		if (kerConnectionDetails.getExposedUrl().getUserInfo() != null) {
			this.remoteKerUri = stripUserInfoFromURI(kerConnectionDetails.getExposedUrl());
			String[] userInfo = kerConnectionDetails.getExposedUrl().getUserInfo().split(":");
			if (userInfo.length == 2) {
				String basicAuthUser = userInfo[0];
				String basicAuthPass = userInfo[1];
				LOG.debug("Configuring password authentication for HTTP client to {}", this.remoteKerUri);
				builder.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(basicAuthUser, basicAuthPass.toCharArray());
					}
				});
			} else {
				throw new IllegalArgumentException(
						"Found user information in remote KER URL, but it does not have two parts. Make sure you don't use a colon inside the parts.");
			}
		} else {
			this.remoteKerUri = kerConnectionDetails.getExposedUrl();
		}

		this.httpClient = builder.connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT)).build();

		objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).findAndRegisterModules()
				.setDateFormat(new RFC3339DateFormat());
	}

	public URI getRemoteKerUri() {
		return this.remoteKerUri;
	}

	private void noError() {
		this.errorCounter = 0;
		this.tryAgainAfter = null;
	}

	private int errorOccurred() {
		this.errorCounter++;
		int waitTime = getWaitTime(this.errorCounter);
		this.tryAgainAfter = LocalDateTime.now().plusMinutes(waitTime);
		return waitTime;
	}

	/**
	 * Contact the Knowledge Engine Runtime to retrieve the latest
	 * {@link KnowledgeEngineRuntimeDetails}
	 */
	private void updateRemoteKerDataFromPeer() {
		try {
			HttpRequest request = HttpRequest.newBuilder(new URI(this.remoteKerUri + "/runtimedetails"))
					.header("Content-Type", "application/json").version(Version.HTTP_1_1).GET().build();

			HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				KnowledgeEngineRuntimeDetails runtimeDetails = objectMapper.readValue(response.body(),
						KnowledgeEngineRuntimeDetails.class);
				LOG.info("Successfully received runtimedetails from " + this.remoteKerUri + " with "
						+ runtimeDetails.getSmartConnectorIds().size() + " Smart Connectors: "
						+ runtimeDetails.getSmartConnectorIds());
				// TODO validate
				this.remoteKerDetails = runtimeDetails;
				noError();
			} else {
				this.remoteKerDetails = null;
				int waitTime = errorOccurred();
				LOG.warn(
						"Failed to receive runtimedetails from {}, got status code {}. Trying KER again in {} minutes.",
						this.remoteKerUri, response.statusCode(), waitTime);
			}
		} catch (IOException | URISyntaxException | InterruptedException e) {
			this.remoteKerDetails = null;
			int waitTime = errorOccurred();
			LOG.warn("Failed to receive runtimedetails from " + this.remoteKerConnectionDetails.getId()
					+ ". Trying KER again in " + waitTime + " minutes.");
			LOG.debug("", e);
		}
		dispatcher.notifySmartConnectorsChanged();
	}

	private boolean isAvailable() {
		if (tryAgainAfter != null) {
			boolean after = LocalDateTime.now().isAfter(tryAgainAfter);
			if (after) {
				LOG.info("KER {} available again.", this.remoteKerUri);
			}
			return after;
		} else
			return true;
	}

	public KnowledgeEngineRuntimeDetails getRemoteKerDetails() {
		if (this.isAvailable() && remoteKerDetails == null) {
			updateRemoteKerDataFromPeer();
		}

		return remoteKerDetails;
	}

	private int getWaitTime(int i) {
		if (i == 1)
			return 1;
		else if (i == 2)
			return 2;
		else if (i == 3)
			return 5;
		else if (i == 4)
			return 10;
		else
			return 15;
	}

	public List<URI> getRemoteSmartConnectorIds() {
		List<URI> list = new ArrayList<>();
		KnowledgeEngineRuntimeDetails remoteKerDetails = getRemoteKerDetails();
		if (remoteKerDetails != null) {
			for (String id : remoteKerDetails.getSmartConnectorIds()) {
				try {
					list.add(new URI(id));
				} catch (URISyntaxException e) {
					LOG.warn("Could not parse remote URI", e);
				}
			}
		}

		LOG.debug("Returning {} SCs for {}.", list.size(), this.remoteKerUri);

		return list;
	}

	public boolean representsKnowledgeBase(URI knowledgeBaseId) {
		if (remoteKerDetails == null) {
			return false;
		}
		return remoteKerDetails.getSmartConnectorIds().contains(knowledgeBaseId.toString());
	}

	/**
	 * This method is called when the KER proactively updates its own KerDetails.
	 *
	 * @param kerDetails
	 */
	public void updateKerDetails(KnowledgeEngineRuntimeDetails kerDetails) {
		// TODO implement checks?
		this.remoteKerDetails = kerDetails;
		dispatcher.notifySmartConnectorsChanged();
	}

	public void start() {
		this.updateRemoteKerDataFromPeer();
	}

	public void stop() {
		if (this.isAvailable()) {
			try {
				HttpRequest request = HttpRequest
						.newBuilder(new URI(this.remoteKerUri + "/runtimedetails/"
								+ dispatcher.getKnowledgeDirectoryConnectionManager().getMyKnowledgeDirectoryId()))
						.header("Content-Type", "application/json").version(Version.HTTP_1_1).DELETE().build();

				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					LOG.trace("Successfully said goodbye to {}", this.remoteKerUri);
				} else {
					LOG.warn("Failed to say goodbye to {}, got response {}: {}", this.remoteKerUri,
							response.statusCode(), response.body());
				}
			} catch (IOException | URISyntaxException | InterruptedException e) {
				LOG.warn("Failed to say goodbye to " + remoteKerConnectionDetails.getId());
				LOG.debug("", e);
			}
		} else
			LOG.warn("Still ignoring KER {}.", this.remoteKerUri);

		// if someone calls this stop method, all smart connectors should be removed
		// from the other knowledge base store. We do this by removing the ker details
		// and calling this method.
		this.remoteKerDetails = null;
		dispatcher.notifySmartConnectorsChanged();
	}

	public void sendToRemoteSmartConnector(KnowledgeMessage message) throws IOException {
		assert (getRemoteKerDetails() == null ? true
				: getRemoteKerDetails().getSmartConnectorIds().contains(message.getToKnowledgeBase().toString()));

		if (this.isAvailable()) {

			try {
				String jsonMessage = objectMapper.writeValueAsString(MessageConverter.toJson(message));
				HttpRequest request = HttpRequest
						.newBuilder(new URI(this.remoteKerUri + getPathForMessageType(message)))
						.header("Content-Type", "application/json").version(Version.HTTP_1_1)
						.POST(BodyPublishers.ofString(jsonMessage)).build();

				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());

				if (response.statusCode() == 202) {
					this.noError();
					LOG.trace("Successfully sent message {} to {}", message.getMessageId(), this.remoteKerUri);
				} else {
					int time = this.errorOccurred();
					LOG.warn("Ignoring KER {} for {} minutes. Failed to send message {} to {}, got response {}: {}",
							this.remoteKerUri, time, message.getMessageId(), this.remoteKerUri, response.statusCode(),
							response.body());
					throw new IOException("Message not accepted by remote host, status code " + response.statusCode()
							+ ", body " + response.body());
				}
			} catch (JsonProcessingException | URISyntaxException | InterruptedException e) {
				int time = this.errorOccurred();
				LOG.warn("Ignoring KER {} for {} minutes.", this.remoteKerUri, time);
				throw new IOException("Could not send message to remote SmartConnector.", e);
			} catch (IOException e) {
				int time = this.errorOccurred();
				LOG.warn("Ignoring KER {} for {} minutes.", this.remoteKerUri, time);
				throw e;
			}
		} else {
			LOG.warn("Still ignoring KER {}.", this.remoteKerUri);
			throw new IOException("KER " + this.remoteKerUri + " is currently unavailable. Trying again later.");
		}
	}

	public void sendMyKerDetailsToPeer(KnowledgeEngineRuntimeDetails details) {
		if (this.isAvailable()) {
			try {
				String jsonMessage = objectMapper.writeValueAsString(details);
				HttpRequest request = HttpRequest.newBuilder(new URI(this.remoteKerUri + "/runtimedetails"))
						.header("Content-Type", "application/json").version(Version.HTTP_1_1)
						.POST(BodyPublishers.ofString(jsonMessage)).build();

				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					this.noError();
					LOG.trace("Successfully sent updated KnowledgeEngineRuntimeDetails to {}", this.remoteKerUri);
				} else {
					this.remoteKerDetails = null;
					this.errorOccurred();
					LOG.warn("Failed to send updated KnowledgeEngineRuntimeDetails to {}, got response {}: {}",
							this.remoteKerUri, response.statusCode(), response.body());
				}
			} catch (IOException | URISyntaxException | InterruptedException e) {
				this.remoteKerDetails = null;
				this.errorOccurred();
				LOG.warn("Failed to send updated KnowledgeEngineRuntimeDetails to "
						+ remoteKerConnectionDetails.getId());
				LOG.debug("", e);
			}
		} else
			LOG.warn("Still ignoring KER {}.", this.remoteKerUri);
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
