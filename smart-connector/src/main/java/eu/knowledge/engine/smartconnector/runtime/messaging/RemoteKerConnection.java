package eu.knowledge.engine.smartconnector.runtime.messaging;

import static eu.knowledge.engine.smartconnector.runtime.messaging.Utils.stripUserInfoFromURI;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * How many seconds the HttpClient waits for a HTTP response when sending a HTTP
	 * request. Default 5 seconds.
	 */
	private static final String CONF_KEY_HTTP_TIMEOUT = "KE_HTTP_TIMEOUT";
	private static final int DEFAULT_HTTP_TIMEOUT = 5;

	public static final Logger LOG = LoggerFactory.getLogger(RemoteKerConnection.class);

	private final KnowledgeEngineRuntimeConnectionDetails remoteKerConnectionDetails;
	private final URI remoteKerUri;
	private KnowledgeEngineRuntimeDetails remoteKerDetails;
	private final MessageDispatcher dispatcher;

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	private LocalDateTime tryAgainAfter = null;
	private int errorCounter = 0;
	private LocalDateTime logStillIgnoringAfter = null;
	private String authToken;
	private String validationEndpoint;

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

		int httpTimeout = getHttpTimeout();

		this.httpClient = builder.connectTimeout(Duration.ofSeconds(httpTimeout)).build();

		objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).findAndRegisterModules()
				.setDateFormat(new RFC3339DateFormat());

		FileInputStream configReader;
		try {
			configReader = new FileInputStream("./edc.properties");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		Properties properties = new Properties();
		try {
			properties.load(configReader);
			configReader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		validationEndpoint = properties.getProperty("tokenValidationEndpoint");
		authToken = properties.getProperty("authorizationToken");
	}

	private int getHttpTimeout() {
		return Integer.parseInt(this.getConfigProperty(CONF_KEY_HTTP_TIMEOUT, Integer.toString(DEFAULT_HTTP_TIMEOUT)));
	}

	public URI getRemoteKerUri() {
		return this.remoteKerUri;
	}

	private void noError() {
		this.errorCounter = 0;
		this.tryAgainAfter = null;
		this.logStillIgnoringAfter = null;
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
					.headers("Content-Type", "application/json",
							"Authorization", authToken).GET().build();

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
		} catch (IOException | URISyntaxException | InterruptedException | IllegalArgumentException e) {
			this.remoteKerDetails = null;
			int waitTime = errorOccurred();
			LOG.warn("Failed to receive runtimedetails from {}, got error '{}'. Trying KER again in {} minutes.",
					this.remoteKerConnectionDetails.getId(), e.getMessage(), waitTime);
			LOG.debug("", e);
		}
		dispatcher.notifySmartConnectorsChanged();
	}

	public boolean isAvailable() {
		if (tryAgainAfter != null) {
			boolean after = LocalDateTime.now().isAfter(tryAgainAfter);
			if (after) {
				LOG.info("KER {} available again.", this.remoteKerUri);
				this.tryAgainAfter = null;
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
				String ker_id = URLEncoder.encode(dispatcher.getMyKnowledgeEngineRuntimeDetails().getRuntimeId(), StandardCharsets.UTF_8);
				HttpRequest request = HttpRequest
						.newBuilder(new URI(this.remoteKerUri + "/runtimedetails/"
								+ dispatcher.getKnowledgeDirectoryConnectionManager().getMyKnowledgeDirectoryId()))
						.headers("Content-Type", "application/json",
								"Authorization", authToken).DELETE().build();

				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					LOG.trace("Successfully said goodbye to {}", this.remoteKerUri);
				} else {
					this.remoteKerDetails = null;
					LOG.warn("Failed to say goodbye to {}, got response {}: {}", this.remoteKerUri,
							response.statusCode(), response.body());
				}
			} catch (IOException | URISyntaxException | InterruptedException | IllegalArgumentException e) {
				this.remoteKerDetails = null;
				LOG.warn("Failed to say goodbye to {}, get error '{}'", remoteKerConnectionDetails.getId(),
						e.getMessage());
				LOG.debug("", e);
			}
		} else
			logStillIgnoring();

		// if someone calls this stop method, all smart connectors should be removed
		// from the other knowledge base store. We do this by removing the ker details
		// and calling this method.
		this.remoteKerDetails = null;
		dispatcher.notifySmartConnectorsChanged();
	}

	/**
	 * To prevent many "Still ignoring" messages, we only log them once a minute.
	 */
	private void logStillIgnoring() {
		if (logStillIgnoringAfter == null || logStillIgnoringAfter.isBefore(LocalDateTime.now())) {
			LOG.warn("Still ignoring KER {}.", this.remoteKerUri);
			logStillIgnoringAfter = LocalDateTime.now().plusMinutes(1);
		}
	}

	public void sendToRemoteSmartConnector(KnowledgeMessage message) throws IOException {
		assert (getRemoteKerDetails() == null ? true
				: getRemoteKerDetails().getSmartConnectorIds().contains(message.getToKnowledgeBase().toString()));

		if (this.isAvailable()) {

			try {
				String jsonMessage = objectMapper.writeValueAsString(MessageConverter.toJson(message));
				HttpRequest request = HttpRequest
						.newBuilder(new URI(this.remoteKerUri + getPathForMessageType(message)))
						.headers("Content-Type", "application/json",
								"Authorization", authToken)
						.POST(BodyPublishers.ofString(jsonMessage)).build();
				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());

				if (response.statusCode() == 202) {
					this.noError();
					LOG.trace("Successfully sent message {} to {}", message.getMessageId(), this.remoteKerUri);
				} else {
					this.remoteKerDetails = null;
					int time = this.errorOccurred();
					LOG.warn("Ignoring KER {} for {} minutes. Failed to send message {} to {}, got response {}: {}",
							this.remoteKerUri, time, message.getMessageId(), this.remoteKerUri, response.statusCode(),
							response.body());
					this.dispatcher.notifySmartConnectorsChanged();
					throw new IOException("Message not accepted by remote host, status code " + response.statusCode()
							+ ", body " + response.body());
				}
			} catch (URISyntaxException | InterruptedException | IOException | IllegalArgumentException e) {
				this.remoteKerDetails = null;
				int time = this.errorOccurred();
				LOG.warn("Ignoring KER {} for {} minutes. Error '{}' occurred.", this.remoteKerUri, time,
						e.getMessage());
				this.dispatcher.notifySmartConnectorsChanged();
				throw new IOException(e);
			}
		} else {
			logStillIgnoring();
			throw new IOException("KER " + this.remoteKerUri + " is currently unavailable. Trying again later.");
		}
	}

	public void sendMyKerDetailsToPeer(KnowledgeEngineRuntimeDetails details) {
		if (this.isAvailable()) {
			try {
				String jsonMessage = objectMapper.writeValueAsString(details);
				HttpRequest request = HttpRequest.newBuilder(new URI(this.remoteKerUri + "/runtimedetails"))
						.headers("Content-Type", "application/json",
								"Authorization", authToken)
						.POST(BodyPublishers.ofString(jsonMessage)).build();

				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					this.noError();
					LOG.trace("Successfully sent updated KnowledgeEngineRuntimeDetails to {}", this.remoteKerUri);
				} else {
					this.remoteKerDetails = null;
					int time = this.errorOccurred();
					this.dispatcher.notifySmartConnectorsChanged();
					LOG.warn(
							"Ignoring KER {} for {} minutes. Failed to send updated KnowledgeEngineRuntimeDetails, got response {}: {}",
							this.remoteKerUri, time, response.statusCode(), response.body());
				}
			} catch (IOException | URISyntaxException | InterruptedException | IllegalArgumentException e) {
				this.remoteKerDetails = null;
				int time = this.errorOccurred();
				this.dispatcher.notifySmartConnectorsChanged();
				LOG.warn(
						"Ignoring KER {} for {} minutes. Failed to send updated KnowledgeEngineRuntimeDetails due to '{}'",
						this.remoteKerUri, time, e.getMessage());
				LOG.debug("", e);
			}
		} else
			logStillIgnoring();
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

	public String getConfigProperty(String key, String defaultValue) {
		// We might replace this with something a bit more fancy in the future...
		String value = System.getenv(key);
		if (value == null) {
			value = defaultValue;
			LOG.trace("No value for the configuration parameter '{}' was provided, using the default value '{}'", key,
					defaultValue);
		}
		return value;
	}

	public boolean hasConfigProperty(String key) {
		return System.getenv(key) != null;
	}
	
	public boolean checkAuthorizationToken(String authorizationToken) {
		if (validationEndpoint != null) {
			LOG.info("Contacting validation endpoint {}", validationEndpoint);
			HttpRequest request = null;
			try {
				request = HttpRequest.newBuilder(new URI(validationEndpoint))
						.headers("Content-Type", "application/json",
								"Authorization", authorizationToken).GET().build();
			} catch (URISyntaxException e) {
				LOG.warn("Invalid URI for the validationEndpoint: "+validationEndpoint);
			}

			try {
				HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					return true;
				}
			} catch (IOException | InterruptedException e) {
				LOG.error("Encountered a problem during authenticating the EDC token");
			}

        }
		return false;
	}
}
