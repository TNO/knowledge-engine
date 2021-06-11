package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorImpl;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.SmartConnectorEndpoint;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.SmartConnectorRegistryListener;

/**
 * The class is responsible for detecting new or removed local
 * {@link SmartConnector}s (using the {@link SmartConnectorRegistryListener})
 * and creating or deleting the {@link LocalSmartConnectorConnection} each local
 * {@link SmartConnector}.
 */
public class LocalSmartConnectorConnectionManager implements SmartConnectorRegistryListener {

	private final Map<URI, LocalSmartConnectorConnection> localSmartConnectorConnections = new ConcurrentHashMap<>();
	private final DistributedMessageDispatcher messageDispatcher;

	public LocalSmartConnectorConnectionManager(DistributedMessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public void start() {
		// Start listening for changes in local smart connectors and connect local smart
		// connectors that already exist
		KeRuntime.localSmartConnectorRegistry().addListener(this);
		for (SmartConnectorImpl sc : KeRuntime.localSmartConnectorRegistry().getSmartConnectors()) {
			this.smartConnectorAdded(sc);
		}
	}

	public void stop() {
		// Stop listening for local changes and say goodbye to the Smart Connectors
		KeRuntime.localSmartConnectorRegistry().removeListener(this);
		this.localSmartConnectorConnections.values().forEach(LocalSmartConnectorConnection::stop);
		this.localSmartConnectorConnections.clear();
	}

	@Override
	public void smartConnectorAdded(SmartConnectorImpl smartConnector) {
		// Create a new LocalSmartConnectorMessageReceiver and attach it
		SmartConnectorEndpoint endpoint = smartConnector.getSmartConnectorEndpoint();
		LocalSmartConnectorConnection connection = this.localSmartConnectorConnections
				.put(endpoint.getKnowledgeBaseId(), new LocalSmartConnectorConnection(messageDispatcher, endpoint));
		connection.start();
		this.messageDispatcher.getRemoteSmartConnectorConnectionsManager().notifyChangedLocalSmartConnectors();
	}

	// Remove the LocalSmartConnectorMessageReceiver and detach it
	@Override
	public void smartConnectorRemoved(SmartConnectorImpl smartConnector) {
		LocalSmartConnectorConnection connection = localSmartConnectorConnections
				.remove(smartConnector.getKnowledgeBaseId());
		connection.stop();
		this.messageDispatcher.getRemoteSmartConnectorConnectionsManager().notifyChangedLocalSmartConnectors();
	}

	public LocalSmartConnectorConnection getLocalSmartConnectorConnection(URI knowledgeBaseId) {
		return this.localSmartConnectorConnections.get(knowledgeBaseId);
	}

	public List<URI> getLocalSmartConnectorIds() {
		return new ArrayList<>(localSmartConnectorConnections.keySet());
	}

}
