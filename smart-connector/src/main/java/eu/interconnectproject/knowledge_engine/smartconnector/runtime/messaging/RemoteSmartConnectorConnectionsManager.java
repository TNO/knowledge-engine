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
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.kd.model.KnowledgeEngineRuntime;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

/**
 * The class is responsible for detecting new or removed remote
 * {@link SmartConnector}s (using the
 * {@link SmartConnectorManagementApiService}) and creating or deleting the
 * {@link RemoteKerConnectionManager} for each remote runtime. In addition, it
 * is also responsible for notifying other KnowledgeEngineRuntimes of local
 * changes.
 */
public class RemoteSmartConnectorConnectionsManager extends SmartConnectorManagementApiService {

	private static final int KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL = 60;
	private final RemoteMessageReceiver messageReceiver = new RemoteMessageReceiver();
	private final Map<String, RemoteKerConnectionManager> messageSenders = new ConcurrentHashMap<>();
	private ScheduledFuture<?> scheduledFuture;
	private final DistributedMessageDispatcher distributedMessageDispatcher;

	public RemoteSmartConnectorConnectionsManager(DistributedMessageDispatcher distributedMessageDispatcher) {
		this.distributedMessageDispatcher = distributedMessageDispatcher;
	}

	public void start() {
		scheduledFuture = KeRuntime.executorService().scheduleAtFixedRate(() -> {
			queryKnowledgeDirectory();
		}, 0, KNOWLEDGE_DIRECTORY_UPDATE_INTERVAL, TimeUnit.SECONDS);
	}

	private synchronized void queryKnowledgeDirectory() {
		List<KnowledgeEngineRuntime> knowledgeEngineRuntimes = distributedMessageDispatcher
				.getKnowledgeDirectoryConnectionManager().getKnowledgeEngineRuntimes();
		// Check if there are new KERs
		for (KnowledgeEngineRuntime knowledgeEngineRuntime : knowledgeEngineRuntimes) {
			if (!messageSenders.containsKey(knowledgeEngineRuntime.getId())) {
				// This must be a new remote KER
				RemoteKerConnectionManager messageSender = new RemoteKerConnectionManager(knowledgeEngineRuntime);
				messageSenders.put(knowledgeEngineRuntime.getId(), messageSender);
				messageSender.start();
			}
		}
		// Check if there are KERs that need to be removed
		List<String> kerIds = knowledgeEngineRuntimes.stream().map(ker -> ker.getId()).collect(Collectors.toList());
		for (Iterator<Entry<String, RemoteKerConnectionManager>> it = messageSenders.entrySet().iterator(); it
				.hasNext();) {
			Entry<String, RemoteKerConnectionManager> e = it.next();
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

	public SmartConnectorMessageSender getMessageSender(URI toKnowledgeBase) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response runtimedetailsGet(SecurityContext securityContext) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response runtimedetailsPost(KnowledgeEngineRuntimeDetails knowledgeEngineRuntimeDetails,
			SecurityContext securityContext) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response runtimedetailsKerIdDelete(String kerId, SecurityContext securityContext) throws NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Notify other KnowledgeEngineRuntimes that something changed locally. Called
	 * directly by the {@link LocalSmartConnectorConnectionsManager} after it made
	 * its own updates.
	 */
	public void notifyChangedLocalSmartConnectors() {
		KnowledgeEngineRuntimeDetails runtimeDetails = KeRuntime.getMessageDispatcher()
				.getKnowledgeEngineRuntimeDetails();
		// TODO
	}

	public RemoteMessageReceiver getMessageReceiver() {
		return messageReceiver;
	}

}
