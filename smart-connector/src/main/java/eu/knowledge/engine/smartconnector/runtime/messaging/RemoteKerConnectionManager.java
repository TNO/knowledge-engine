package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.edc.EdcConnectorService;
import eu.knowledge.engine.smartconnector.edc.ParticipantProperties;
import eu.knowledge.engine.smartconnector.edc.TransferProcess;
import eu.knowledge.engine.smartconnector.runtime.KeRuntime;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.NotFoundException;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.SmartConnectorManagementApiService;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.KnowledgeEngineRuntimeDetails;
import eu.knowledge.engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * The class is responsible for detecting new or removed remote
 * {@link SmartConnector}s (using the
 * {@link SmartConnectorManagementApiService}) and creating or deleting the
 * {@link RemoteKerConnection} for each remote runtime. In addition, it is also
 * responsible for notifying other KnowledgeEngineRuntimes of local changes.
 */
public class RemoteKerConnectionManager extends SmartConnectorManagementApiService {

	private static final Logger LOG = LoggerFactory.getLogger(RemoteKerConnectionManager.class);

	private static final int KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL = 60;
	private static final int KNOWLEDGE_DIRECTORY_UPDATE_COOLDOWN = 2;
	private final RemoteMessageReceiver messageReceiver;
	private final Map<String, RemoteKerConnection> remoteKerConnections = new ConcurrentHashMap<>();
	private final Map<String, RemoteKerConnection> unavailableRemoteKerConnections = new ConcurrentHashMap<>();
	private ScheduledFuture<?> scheduledScheduleFuture;
	private ScheduledFuture<?> scheduledKnowledgeDirectoryQueryFuture;
	private final MessageDispatcher messageDispatcher;
	private Date knowledgeDirectoryUpdateCooldownEnds = null;
	private EdcConnectorService edcService = null;
	private boolean useEdc;

	public RemoteKerConnectionManager(MessageDispatcher messageDispatcher, URI myExposedUrl, boolean useEdc) {
		this.messageDispatcher = messageDispatcher;
		messageReceiver = new RemoteMessageReceiver(messageDispatcher);
		this.useEdc = useEdc;

		if (this.useEdc) {
			this.edcService = new EdcConnectorService(myExposedUrl);
		}
	}

	public void start() {
		// Make a schedule to schedule a knowledge directory update every minute.
		scheduledScheduleFuture = KeRuntime.executorService().scheduleAtFixedRate(() -> {
			try {
				scheduleQueryKnowledgeDirectory();
			} catch (Throwable t) {
				LOG.error("", t);
			}
		}, 5, KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL, TimeUnit.SECONDS);

		// configure our EDC Connector with the TKE asset
		if (useEdc) {
			edcService.configureConnector();
		}
	}

	public void scheduleQueryKnowledgeDirectory() {
		if (this.scheduledKnowledgeDirectoryQueryFuture != null
				&& !this.scheduledKnowledgeDirectoryQueryFuture.isDone()) {
			// There was already a scheduled update, so we don't have to do anything.
			LOG.debug(
					"It was requested to schedule a query to the Knowledge Directory but there was already a scheduled query! Doing nothing. ");
			return;
		}

		var now = new Date();

		if (knowledgeDirectoryUpdateCooldownEnds == null
				|| knowledgeDirectoryUpdateCooldownEnds.getTime() - now.getTime() < 0) {
			// Cooldown already ended: schedule it on the KeRuntime right away.
			LOG.debug("Scheduling to query the Knowledge Directory right away.");
			this.scheduledKnowledgeDirectoryQueryFuture = KeRuntime.executorService().schedule(() -> {
				try {
					queryKnowledgeDirectory();
				} catch (Throwable t) {
					LOG.error("", t);
				}
				this.scheduledKnowledgeDirectoryQueryFuture = null;
			}, 0, TimeUnit.MILLISECONDS);
		} else {
			// Cooldown not yet ended: schedule to update when the cooldown ends.
			LOG.debug("Scheduling to query the Knowledge Directory when the cooldown ends (in {} ms).",
					knowledgeDirectoryUpdateCooldownEnds.getTime() - now.getTime());

			this.scheduledKnowledgeDirectoryQueryFuture = KeRuntime.executorService().schedule(() -> {
				queryKnowledgeDirectory();
				this.scheduledKnowledgeDirectoryQueryFuture = null;
			}, knowledgeDirectoryUpdateCooldownEnds.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
		}
	}

	private synchronized void queryKnowledgeDirectory() {
		List<KnowledgeEngineRuntimeConnectionDetails> kerConnectionDetails;
		try {
			LOG.info("Querying Knowledge Directory for new peers");
			kerConnectionDetails = messageDispatcher.getKnowledgeDirectoryConnectionManager()
					.getOtherKnowledgeEngineRuntimeConnectionDetails();
			// Check if there are new KERs
		} catch (Exception e) {
			LOG.error("Error while querying the Knowledge Directory", e);
			return;
		}

		for (KnowledgeEngineRuntimeConnectionDetails knowledgeEngineRuntime : kerConnectionDetails) {
			if (!remoteKerConnections.containsKey(knowledgeEngineRuntime.getId())) {
				// This must be a new remote KER
				LOG.info("Discovered new peer " + knowledgeEngineRuntime.getId());

				RemoteKerConnection messageSender;
				if (useEdc) {
					URI counterPartyParticipantId = knowledgeEngineRuntime.getEdcParticipantId();
					ParticipantProperties participant = new ParticipantProperties(
						counterPartyParticipantId,
						knowledgeEngineRuntime.getEdcConnectorUrl(),
						knowledgeEngineRuntime.getEdcDataPlaneUrl()
					);
					this.edcService.registerParticipant(participant);
					TransferProcess transferProcess = this.edcService.createTransferProcess(counterPartyParticipantId);

					messageSender = new RemoteKerConnection(messageDispatcher, knowledgeEngineRuntime, transferProcess);
				} else {
					messageSender = new RemoteKerConnection(messageDispatcher, knowledgeEngineRuntime, null);
				}
				remoteKerConnections.put(knowledgeEngineRuntime.getId(), messageSender);
				messageSender.start();
				messageSender.sendMyKerDetailsToPeer(messageDispatcher.getMyKnowledgeEngineRuntimeDetails());
			}
		}
		// Check if there are KERs that need to be removed
		List<String> kerIds = kerConnectionDetails.stream().map(ker -> ker.getId()).collect(Collectors.toList());
		for (Iterator<Entry<String, RemoteKerConnection>> it = remoteKerConnections.entrySet().iterator(); it
				.hasNext();) {
			Entry<String, RemoteKerConnection> e = it.next();

			// deal with unavailable remote kers
			if (e.getValue().isAvailable() && this.unavailableRemoteKerConnections.containsKey(e.getKey())) {
				// available again so make sure we get its current SCs
				this.unavailableRemoteKerConnections.remove(e.getKey());
				e.getValue().getRemoteKerDetails();
			}

			if (!e.getValue().isAvailable()) {
				if (!this.unavailableRemoteKerConnections.containsKey(e.getKey())) {
					// recently became unavailable
					this.unavailableRemoteKerConnections.put(e.getKey(), e.getValue());
				}
			}

			if (!kerIds.contains(e.getKey())) {
				// According to the Knowledge Directory, this KER doesn't exist (anymore)
				LOG.info("Removing peer that is now gone: {}", e.getValue().getRemoteKerUri());
				it.remove();
				this.unavailableRemoteKerConnections.remove(e.getKey());

				// make sure all SCs are notified correctly. We use fake/empty details for that.
				KnowledgeEngineRuntimeDetails details = new KnowledgeEngineRuntimeDetails();
				details.setRuntimeId(e.getValue().getRemoteKerUri().toString());
				details.setSmartConnectorIds(new ArrayList<>());
				e.getValue().updateKerDetails(details);

			}
		}
		this.knowledgeDirectoryUpdateCooldownEnds = new Date(
				new Date().getTime() + KNOWLEDGE_DIRECTORY_UPDATE_COOLDOWN * 1000);
	}

	public void stop() {
		this.scheduledScheduleFuture.cancel(false);

		// let other KERs know we are stopping
		for (RemoteKerConnection remoteKerConnection : this.remoteKerConnections.values()) {
			remoteKerConnection.stop();
		}
	}

	public RemoteKerConnection getRemoteKerConnection(URI toKnowledgeBase) {
		for (RemoteKerConnection remoteKerConnection : this.remoteKerConnections.values()) {
			if (remoteKerConnection.representsKnowledgeBase(toKnowledgeBase)) {
				return remoteKerConnection;
			}
		}
		return null;
	}

	/**
	 * Another KER would like to know our {@link KnowledgeEngineRuntimeDetails}.
	 */
	@Override
	public Response runtimedetailsGet(String authorizationToken, SecurityContext securityContext)
			throws NotFoundException {
		KnowledgeEngineRuntimeDetails runtimeDetails = messageDispatcher.getMyKnowledgeEngineRuntimeDetails();
		return Response.status(200).entity(runtimeDetails).build();
	}

	/**
	 * Another KER notifies us that its new or its
	 * {@link KnowledgeEngineRuntimeDetails} have changed.
	 */
	@Override
	public Response runtimedetailsPost(String authorizationToken,
			KnowledgeEngineRuntimeDetails knowledgeEngineRuntimeDetails, SecurityContext securityContext)
			throws NotFoundException {
		RemoteKerConnection remoteKerConnection = remoteKerConnections
				.get(knowledgeEngineRuntimeDetails.getRuntimeId());
		if (remoteKerConnection == null) {
			// It is a new KER. We don't process the data now, but trigger a new knowledge
			// directory query, which should trigger a GET.
			KeRuntime.executorService().execute(() -> scheduleQueryKnowledgeDirectory());
		} else {
			// The KER has changed its details
			LOG.info("Received new or removed Smart Connectors from peer " + remoteKerConnection.getRemoteKerUri()
					+ " with " + knowledgeEngineRuntimeDetails.getSmartConnectorIds().size() + " smart connectors");
			remoteKerConnection.updateKerDetails(knowledgeEngineRuntimeDetails);
		}
		return Response.status(200).build();
	}

	/**
	 * Another KER lets us know it will leave.
	 */
	@Override
	public Response runtimedetailsKerIdDelete(String authorizationToken, String kerId, SecurityContext securityContext)
			throws NotFoundException {
		RemoteKerConnection kerConnection = remoteKerConnections.remove(kerId);
		if (kerConnection == null) {
			// That one didn't exist
			return Response.status(404).build();
		} else {
			// make sure all SCs are notified correctly. We use fake/empty details for that.
			KnowledgeEngineRuntimeDetails details = new KnowledgeEngineRuntimeDetails();
			details.setRuntimeId(kerConnection.getRemoteKerUri().toString());
			details.setSmartConnectorIds(new ArrayList<>());
			kerConnection.updateKerDetails(details);

			// Done!
			return Response.status(204).build();
		}
	}

	/**
	 * Notify other KnowledgeEngineRuntimes that something changed locally. Called
	 * directly by the {@link LocalSmartConnectorConnectionManager} after it made
	 * its own updates.
	 */
	public void notifyChangedLocalSmartConnectors() {
		KnowledgeEngineRuntimeDetails runtimeDetails = messageDispatcher.getMyKnowledgeEngineRuntimeDetails();
		LOG.info("Notifying " + this.remoteKerConnections.size()
				+ " peer(s) of new or removed Smart Connectors, there are now "
				+ runtimeDetails.getSmartConnectorIds().size() + " smart connectors");
		for (RemoteKerConnection remoteKerConnection : this.remoteKerConnections.values()) {
			remoteKerConnection.sendMyKerDetailsToPeer(runtimeDetails);
		}
	}

	public RemoteMessageReceiver getMessageReceiver() {
		return messageReceiver;
	}

	public List<URI> getRemoteSmartConnectorIds() {
		List<URI> list = new ArrayList<>();
		for (RemoteKerConnection remoteKerConnection : remoteKerConnections.values()) {
			list.addAll(remoteKerConnection.getRemoteSmartConnectorIds());
		}
		return list;
	}

	public ParticipantProperties getMyEdcProperties() {
		return this.edcService.getMyProperties();
	}
}
