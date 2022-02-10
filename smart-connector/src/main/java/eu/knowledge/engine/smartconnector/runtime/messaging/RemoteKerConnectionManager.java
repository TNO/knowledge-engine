package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.runtime.KeRuntime;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.NotFoundException;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.api.SmartConnectorManagementApiService;
import eu.knowledge.engine.smartconnector.runtime.messaging.inter_ker.model.KnowledgeEngineRuntimeDetails;
import eu.knowledge.engine.smartconnector.runtime.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;

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
	private final RemoteMessageReceiver messageReceiver;
	private final Map<String, RemoteKerConnection> remoteKerConnections = new ConcurrentHashMap<>();
	private ScheduledFuture<?> scheduledFuture;
	private final MessageDispatcher messageDispatcher;

	public RemoteKerConnectionManager(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
		messageReceiver = new RemoteMessageReceiver(messageDispatcher);
	}

	public void start() {
		scheduledFuture = KeRuntime.executorService().scheduleAtFixedRate(() -> {
			try {
				queryKnowledgeDirectory();
			} catch (Exception e) {
				LOG.error("Error while querying the Knowledge Directory", e);
			}
		}, 5, KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL, TimeUnit.SECONDS);
	}

	public synchronized void queryKnowledgeDirectory() {
		List<KnowledgeEngineRuntimeConnectionDetails> kerConnectionDetails = messageDispatcher
				.getKnowledgeDirectoryConnectionManager().getOtherKnowledgeEngineRuntimeConnectionDetails();
		// Check if there are new KERs
		for (KnowledgeEngineRuntimeConnectionDetails knowledgeEngineRuntime : kerConnectionDetails) {
			if (!remoteKerConnections.containsKey(knowledgeEngineRuntime.getId())) {
				// This must be a new remote KER
				LOG.info("Discovered new peer " + knowledgeEngineRuntime.getId());
				RemoteKerConnection messageSender = new RemoteKerConnection(messageDispatcher, knowledgeEngineRuntime);
				remoteKerConnections.put(knowledgeEngineRuntime.getId(), messageSender);
				messageSender.start();
			}
		}
		// Check if there are KERs that need to be removed
		List<String> kerIds = kerConnectionDetails.stream().map(ker -> ker.getId()).collect(Collectors.toList());
		for (Iterator<Entry<String, RemoteKerConnection>> it = remoteKerConnections.entrySet().iterator(); it
				.hasNext();) {
			Entry<String, RemoteKerConnection> e = it.next();
			if (!kerIds.contains(e.getKey())) {
				// According the the Knowledge Directory, this KER doesn't exist (anymore)
				LOG.info("Removing peer that is now gone: " + e.getValue().getRemoteKerDetails().getRuntimeId());
				e.getValue().stop();
				it.remove();
			}
		}
	}

	public void stop() {
		this.scheduledFuture.cancel(false);
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
	public Response runtimedetailsGet(SecurityContext securityContext) throws NotFoundException {
		KnowledgeEngineRuntimeDetails runtimeDetails = messageDispatcher.getMyKnowledgeEngineRuntimeDetails();
		return Response.status(200).entity(runtimeDetails).build();
	}

	/**
	 * Another KER notifies us that its new or its
	 * {@link KnowledgeEngineRuntimeDetails} have changed.
	 */
	@Override
	public Response runtimedetailsPost(KnowledgeEngineRuntimeDetails knowledgeEngineRuntimeDetails,
			SecurityContext securityContext) throws NotFoundException {
		RemoteKerConnection remoteKerConnection = remoteKerConnections
				.get(knowledgeEngineRuntimeDetails.getRuntimeId());
		if (remoteKerConnection == null) {
			// It is a new KER. We don't process the data now, but trigger a new knowledge
			// directory query, which should trigger a GET.
			KeRuntime.executorService().execute(() -> queryKnowledgeDirectory());
		} else {
			// The KER has changed its details
			LOG.info("Received new or removed Smart Connectors from peer "
					+ knowledgeEngineRuntimeDetails.getRuntimeId() + " with "
					+ knowledgeEngineRuntimeDetails.getSmartConnectorIds().size() + " smart connectors");
			remoteKerConnection.updateKerDetails(knowledgeEngineRuntimeDetails);
		}
		return Response.status(200).build();
	}

	/**
	 * Another KER lets us know it will leave.
	 */
	@Override
	public Response runtimedetailsKerIdDelete(String kerId, SecurityContext securityContext) throws NotFoundException {
		RemoteKerConnection kerConnection = remoteKerConnections.remove(kerId);
		if (kerConnection == null) {
			// That one didn't exist
			return Response.status(404).build();
		} else {
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

}
