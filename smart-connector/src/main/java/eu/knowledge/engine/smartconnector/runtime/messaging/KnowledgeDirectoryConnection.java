package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.knowledge.engine.smartconnector.runtime.KeRuntime;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.RFC3339DateFormat;
import eu.knowledge.engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

/**
 * The {@link KnowledgeDirectoryConnection} is responsible for providing access
 * to the Knowledge Directory, maintaining the connection and the renewing the
 * lease. It must be started and stopped.
 */
public class KnowledgeDirectoryConnection {

	private static final int RENEW_INTERVAL_SECONDS = 33;

	private final static Logger LOG = org.slf4j.LoggerFactory.getLogger(KnowledgeDirectoryConnection.class);

	private static final String PROTOCOL_VERSION = "1.0.0-SNAPSHOT";

	public static enum State {
		UNREGISTERED, REGISTERED, INTERRUPTED, STOPPING, STOPPED
	}

	private HttpClient httpClient;
	private ObjectMapper objectMapper;
	private String myId;
	private State currentState;
	private final URI kdUrl;
	private final URI myExposedUrl;
	private final Object lock = new Object();

	private ScheduledFuture<?> scheduledFuture;

	public KnowledgeDirectoryConnection(URI kdUrl, URI myExposedUrl) {
		this.kdUrl = kdUrl;
		this.myExposedUrl = myExposedUrl;
		this.currentState = State.UNREGISTERED;
	}

	public void start() {
		// Check state
		if (currentState != State.UNREGISTERED) {
			throw new IllegalStateException(
					"Can only start KnowledgeDirectoryConnectionManager when the state is UNREGISTERED");
		}

		LOG.info("Starting connection with Knowledge Directory at " + kdUrl);

		// Init
		var builder = HttpClient.newBuilder();

		if (kdUrl.getUserInfo() != null) {
			String[] userInfo = kdUrl.getUserInfo().split(":");
			if (userInfo.length == 2) {
				builder.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {

						return new PasswordAuthentication(userInfo[0], userInfo[1].toCharArray());
					}
				});
			}
		}

		httpClient = builder.build();

		objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).findAndRegisterModules()
				.setDateFormat(new RFC3339DateFormat());

		// Schedule automatic register/renew
		scheduledFuture = KeRuntime.executorService().scheduleAtFixedRate(() -> {
			try {
				synchronized (lock) {
					switch (this.currentState) {
					case UNREGISTERED:
						// try to register
						tryRegister();
						break;
					case REGISTERED:
						// try to renew
						tryRenewLease();
						break;
					case INTERRUPTED:
						// try to register again
						tryRegister();
						break;
					case STOPPING:
						break;
					case STOPPED:
						break;
					default:
						break;
					}
				}
			} catch (Exception e) {
				LOG.error("Exception in renew loop of KnowledgeDirectoryConnectionManager", e);
			}
		}, 0, RENEW_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	public void stop() {
		synchronized (lock) {
			if (currentState == State.STOPPING || currentState == State.STOPPED) {
				throw new IllegalStateException(
						"The KnowledgeDirectoryConnectionManager was already trying to stop or stopped");
			}

			LOG.info("Stopping connection with Knowledge Directory at " + kdUrl);

			this.currentState = State.STOPPING;
			scheduledFuture.cancel(true);
			// If unregistering fails we don't care about it, the lease will expire anyway
			tryUnregister();
			this.currentState = State.STOPPED;
		}
	}

	public State getState() {
		return this.currentState;
	}

	public List<KnowledgeEngineRuntimeConnectionDetails> getKnowledgeEngineRuntimeConnectionDetails() {
		if (this.currentState != State.REGISTERED && currentState != State.INTERRUPTED) {
			throw new IllegalStateException(
					"Can only retrieve Knowledge Directory infomation when REGISTERED or INTERRUPETD");
		}
		try {
			URI uri = new URI(kdUrl + "/ker/");
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

			KnowledgeEngineRuntimeConnectionDetails[] result = objectMapper.readValue(response.body(),
					KnowledgeEngineRuntimeConnectionDetails[].class);
			ArrayList<KnowledgeEngineRuntimeConnectionDetails> list = new ArrayList<KnowledgeEngineRuntimeConnectionDetails>();
			Collections.addAll(list, result);
			return list;
		} catch (IOException | InterruptedException | URISyntaxException e) {
			LOG.warn("Was not able to retrieve KnowledgeEngineRuntimeConnectionDetails", e);
			return Collections.emptyList();
		}
	}

	public List<KnowledgeEngineRuntimeConnectionDetails> getOtherKnowledgeEngineRuntimeConnectionDetails() {
		List<KnowledgeEngineRuntimeConnectionDetails> list = new ArrayList<>(
				getKnowledgeEngineRuntimeConnectionDetails());
		list.removeIf(e -> myId.equals(e.getId()));
		return list;
	}

	public String getMyKnowledgeDirectoryId() {
		return this.myId;
	}

	private void tryRegister() {
		if (this.currentState == State.REGISTERED || currentState == State.STOPPED) {
			throw new IllegalStateException("Can only register when NEW or INTERRUPTED");
		}
		KnowledgeEngineRuntimeConnectionDetails ker = new KnowledgeEngineRuntimeConnectionDetails();
		ker.setExposedUrl(myExposedUrl);
		ker.setProtocolVersion(PROTOCOL_VERSION);

		try {
			HttpRequest registerRequest = HttpRequest
					.newBuilder(new URI(kdUrl + "/ker/"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(ker))).build();
			HttpResponse<String> response = httpClient.send(registerRequest, BodyHandlers.ofString());
			int statusCode = response.statusCode();
			if (statusCode == 201) {
				// Registration was successful
				myId = response.body();
				this.currentState = State.REGISTERED;
				LOG.info("Successfully registered at the Knowledge Directory " + kdUrl);
			} else if (statusCode == 400) {
				// Registration was not successful
				this.currentState = State.INTERRUPTED;
				LOG.warn("Could not register at Knowledge Directory " + kdUrl + ", response was: "
						+ response.body());
			} else if (statusCode == 409) {
				// Was already registered
				myId = response.body();
				this.currentState = State.REGISTERED;
				LOG.debug("Tried to register at Knowledge Directory " + kdUrl
						+ ", but was already registered");
				tryRenewLease();
			}
		} catch (IOException | InterruptedException | URISyntaxException e) {
			// Could not register
			this.currentState = State.INTERRUPTED;
			LOG.warn("Could not register at Knowledge Directory " + kdUrl, e);
		}
	}

	private void tryUnregister() {
		if (this.currentState != State.STOPPING) {
			throw new IllegalStateException("Can only unregister when STOPPING");
		} else if (this.myId == null) {
			// We haven't even been properly registered yet, so we can consider
			// ourselves successfully stopped.
			this.currentState = State.STOPPED;
			return;
		}

		try {
			HttpRequest registerRequest = HttpRequest
					.newBuilder(new URI(kdUrl + "/ker/" + urlEncode(myId)))
					.header("Content-Type", "application/json").DELETE().build();
			HttpResponse<String> response = httpClient.send(registerRequest, BodyHandlers.ofString());
			int statusCode = response.statusCode();
			if (statusCode == 200) {
				// Unregister was successful
				this.currentState = State.STOPPED;
				LOG.info("Successfully unregistered at the Knowledge Directory " + kdUrl);
			} else if (statusCode == 404) {
				// Not found, so we're not registered anymore, also fine
				this.currentState = State.STOPPED;
				LOG.info("Could not unregister at Knowledge Directory " + kdUrl + ", response was: "
						+ response.body());
			} else {
				LOG.warn("Unknown status code {} while unregistering the KER", statusCode);
			}
		} catch (IOException | InterruptedException | URISyntaxException e) {
			// Could not register
			this.currentState = State.STOPPING;
			LOG.warn("Could not unregister at Knowledge Directory " + kdUrl, e);
		}
	}

	private void tryRenewLease() {
		if (this.currentState != State.REGISTERED) {
			throw new IllegalStateException(
					"Can only renew lease of KnowledgeDirectoryConnectionManager when the state is REGISTERED");
		}
		LOG.debug("Attempting a renew of the lease at Knowledge Directory " + kdUrl);
		try {
			HttpRequest registerRequest = HttpRequest
					.newBuilder(new URI(kdUrl + "/ker/" + urlEncode(myId) + "/renew"))
					.header("Content-Type", "application/json").POST(BodyPublishers.noBody()).build();
			HttpResponse<String> response = httpClient.send(registerRequest, BodyHandlers.ofString());
			int statusCode = response.statusCode();
			if (statusCode == 204) {
				// Renew was successful
				this.currentState = State.REGISTERED;
				LOG.debug("Renewed lease at Knowledge Directory " + kdUrl);
			} else if (statusCode == 404) {
				// Doesn't recognize this KER
				this.currentState = State.INTERRUPTED;
				LOG.info("Could not renew lease at Knowledge Directory " + kdUrl
						+ ", response was: " + response.body());
			} else {
				LOG.warn("Unknown status code {} while calling the renewing the lease on the Knowledge Direcotry",
						statusCode);
			}
		} catch (IOException | InterruptedException | URISyntaxException e) {
			// Could not renew
			this.currentState = State.INTERRUPTED;
			LOG.warn("Could not renew lease at Knowledge Directory " + kdUrl, e);
		}

	}

	private String urlEncode(String urlParameter) {
		return URLEncoder.encode(urlParameter, StandardCharsets.UTF_8);
	}
}
