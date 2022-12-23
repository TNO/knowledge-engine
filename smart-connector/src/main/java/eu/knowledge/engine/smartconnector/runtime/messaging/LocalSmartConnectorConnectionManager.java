package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.impl.RuntimeSmartConnector;
import eu.knowledge.engine.smartconnector.messaging.SmartConnectorEndpoint;
import eu.knowledge.engine.smartconnector.runtime.KeRuntime;
import eu.knowledge.engine.smartconnector.runtime.SmartConnectorRegistryListener;

/**
 * The class is responsible for detecting new or removed local
 * {@link SmartConnector}s (using the {@link SmartConnectorRegistryListener})
 * and creating or deleting the {@link LocalSmartConnectorConnection} each local
 * {@link SmartConnector}.
 */
public class LocalSmartConnectorConnectionManager implements SmartConnectorRegistryListener {

	private final Map<URI, LocalSmartConnectorConnection> localSmartConnectorConnections = new ConcurrentHashMap<>();
	private final MessageDispatcher messageDispatcher;

	public LocalSmartConnectorConnectionManager(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public void start() {
		// Start listening for changes in local smart connectors and connect local smart
		// connectors that already exist
		KeRuntime.localSmartConnectorRegistry().addListener(this);
		for (RuntimeSmartConnector sc : KeRuntime.localSmartConnectorRegistry().getSmartConnectors()) {
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
	public void smartConnectorAdded(RuntimeSmartConnector smartConnector) {
		if (this.messageDispatcher.getKnowledgeBaseIds().contains(smartConnector.getKnowledgeBaseId())) {
			throw new IllegalArgumentException("The smart connector should have a unique knowledge base ID.");
		}
		// Create a new LocalSmartConnectorMessageReceiver and attach it
		SmartConnectorEndpoint endpoint = smartConnector.getSmartConnectorEndpoint();
		LocalSmartConnectorConnection connection = new LocalSmartConnectorConnection(messageDispatcher, endpoint);
		this.localSmartConnectorConnections.put(endpoint.getKnowledgeBaseId(), connection);
		connection.start();
		if (messageDispatcher.runsInDistributedMode()) {
			this.messageDispatcher.getRemoteSmartConnectorConnectionsManager().notifyChangedLocalSmartConnectors();
			this.messageDispatcher.notifySmartConnectorsChanged();
		}
	}

	// Remove the LocalSmartConnectorMessageReceiver and detach it
	@Override
	public void smartConnectorRemoved(RuntimeSmartConnector smartConnector) {
		LocalSmartConnectorConnection connection = localSmartConnectorConnections
				.remove(smartConnector.getKnowledgeBaseId());
		connection.stop();

		if (messageDispatcher.runsInDistributedMode()) {
			this.messageDispatcher.getRemoteSmartConnectorConnectionsManager().notifyChangedLocalSmartConnectors();
			this.messageDispatcher.notifySmartConnectorsChanged();
		}
	}

	public LocalSmartConnectorConnection getLocalSmartConnectorConnection(URI knowledgeBaseId) {
		return this.localSmartConnectorConnections.get(knowledgeBaseId);
	}

	public List<URI> getLocalSmartConnectorIds() {
		return new ArrayList<>(localSmartConnectorConnections.keySet());
	}

}
