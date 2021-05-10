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
 * and creating or deleting the {@link LocalSmartConnectorMessageSender} and the
 * {@link LocalSmartConnectorMessageReceiver} for each local
 * {@link SmartConnector}.
 */
public class LocalSmartConnectorConnectionsManager implements SmartConnectorRegistryListener {

	private final Map<URI, LocalSmartConnectorMessageReceiver> localSmartConnectorMessageReceivers = new ConcurrentHashMap<>();
	private final Map<URI, LocalSmartConnectorMessageSender> localSmartConnectorMessageSenders = new ConcurrentHashMap<>();
	private final DistributedMessageDispatcher messageDispatcher;

	public LocalSmartConnectorConnectionsManager(DistributedMessageDispatcher messageDispatcher) {
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
		this.localSmartConnectorMessageReceivers.values().forEach(LocalSmartConnectorMessageReceiver::stop);
		this.localSmartConnectorMessageReceivers.clear();
	}

	@Override
	public void smartConnectorAdded(SmartConnectorImpl smartConnector) {
		// Create a new LocalSmartConnectorMessageReceiver and attach it
		SmartConnectorEndpoint endpoint = smartConnector.getSmartConnectorEndpoint();
		LocalSmartConnectorMessageReceiver handler = new LocalSmartConnectorMessageReceiver(messageDispatcher,
				endpoint);
		this.localSmartConnectorMessageReceivers.put(endpoint.getKnowledgeBaseId(), handler);
		this.localSmartConnectorMessageSenders.put(endpoint.getKnowledgeBaseId(),
				new LocalSmartConnectorMessageSender(endpoint));
		handler.start();
		this.messageDispatcher.getRemoteSmartConnectorConnectionsManager().notifyChangedLocalSmartConnectors();
	}

	@Override
	// Remove the LocalSmartConnectorMessageReceiver and detach it
	public void smartConnectorRemoved(SmartConnectorImpl smartConnector) {
		LocalSmartConnectorMessageReceiver handler = this.localSmartConnectorMessageReceivers
				.remove(smartConnector.getKnowledgeBaseId());
		handler.stop();
		localSmartConnectorMessageSenders.remove(smartConnector.getKnowledgeBaseId());
		this.messageDispatcher.getRemoteSmartConnectorConnectionsManager().notifyChangedLocalSmartConnectors();
	}

	public SmartConnectorMessageSender getMessageSender(URI knowledgeBaseId) {
		return this.localSmartConnectorMessageSenders.get(knowledgeBaseId);
	}

	public List<URI> getLocalSmartConnectorIds() {
		return new ArrayList<>(localSmartConnectorMessageReceivers.keySet());
	}

}
