package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.net.URI;
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

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.NotFoundException;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.api.SmartConnectorManagementApiService;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.inter_ker.server.model.KnowledgeEngineRuntimeDetails;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.kd.model.KnowledgeEngineRuntimeConnectionDetails;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

/**
 * The class is responsible for detecting new or removed remote
 * {@link SmartConnector}s (using the
 * {@link SmartConnectorManagementApiService}) and creating or deleting the
 * {@link RemoteKerConnection} for each remote runtime. In addition, it is also
 * responsible for notifying other KnowledgeEngineRuntimes of local changes.
 */
public class RemoteKerConnectionManager extends SmartConnectorManagementApiService {

	private static final int KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL = 60;
	private final RemoteMessageReceiver messageReceiver = new RemoteMessageReceiver();
	private final Map<String, RemoteKerConnection> remoteKerConnections = new ConcurrentHashMap<>();
	private ScheduledFuture<?> scheduledFuture;
	private final DistributedMessageDispatcher distributedMessageDispatcher;

	public RemoteKerConnectionManager(DistributedMessageDispatcher distributedMessageDispatcher) {
		this.distributedMessageDispatcher = distributedMessageDispatcher;
	}

	public void start() {
		scheduledFuture = KeRuntime.executorService().scheduleAtFixedRate(() -> {
			queryKnowledgeDirectory();
		}, 0, KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL, TimeUnit.SECONDS);
	}

	private synchronized void queryKnowledgeDirectory() {
		List<KnowledgeEngineRuntimeConnectionDetails> kerConnectionDetails = distributedMessageDispatcher
				.getKnowledgeDirectoryConnectionManager().getOtherKnowledgeEngineRuntimeConnectionDetails();
		// Check if there are new KERs
		for (KnowledgeEngineRuntimeConnectionDetails knowledgeEngineRuntime : kerConnectionDetails) {
			if (!remoteKerConnections.containsKey(knowledgeEngineRuntime.getId())) {
				// This must be a new remote KER
				RemoteKerConnection messageSender = new RemoteKerConnection(knowledgeEngineRuntime);
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
		KnowledgeEngineRuntimeDetails runtimeDetails = distributedMessageDispatcher
				.getMyKnowledgeEngineRuntimeDetails();
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
			remoteKerConnection.updateKerDetails(knowledgeEngineRuntimeDetails);
		}
		return Response.status(200).build();
	}

	/**
	 * Another KER lets un know it will leave.
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
		KnowledgeEngineRuntimeDetails runtimeDetails = distributedMessageDispatcher
				.getMyKnowledgeEngineRuntimeDetails();
		for (RemoteKerConnection remoteKerConnection : this.remoteKerConnections.values()) {
			remoteKerConnection.sendMyKerDetailsToPeer(runtimeDetails);
		}
	}

	public RemoteMessageReceiver getMessageReceiver() {
		return messageReceiver;
	}

}
