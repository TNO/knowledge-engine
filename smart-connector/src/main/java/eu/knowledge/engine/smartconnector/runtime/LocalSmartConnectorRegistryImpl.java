package eu.knowledge.engine.smartconnector.runtime;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.impl.RuntimeSmartConnector;

/**
 * Singleton object that keeps a reference to every SmartConnector object in
 * this JVM.
 */
public class LocalSmartConnectorRegistryImpl implements LocalSmartConnectorRegistry {

	private final Map<URI, RuntimeSmartConnector> smartConnectors = new HashMap<>();
	private final List<SmartConnectorRegistryListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Constructor may only be called by {@link KeRuntime}
	 */
	LocalSmartConnectorRegistryImpl() {
	}

	@Override
	public void register(RuntimeSmartConnector smartConnector) {
		synchronized (smartConnectors) {
			if (smartConnectors.containsKey(smartConnector.getKnowledgeBaseId())) {
				throw new IllegalArgumentException(
						"There already is a smart connector registered with ID " + smartConnector.getKnowledgeBaseId());
			}
			smartConnectors.put(smartConnector.getKnowledgeBaseId(), smartConnector);
		}
		for (SmartConnectorRegistryListener l : listeners) {
			l.smartConnectorAdded(smartConnector);
		}
	};

	@Override
	public void unregister(RuntimeSmartConnector smartConnector) {
		synchronized (smartConnectors) {
			smartConnectors.remove(smartConnector.getKnowledgeBaseId());
		}
		for (SmartConnectorRegistryListener l : listeners) {
			l.smartConnectorRemoved(smartConnector);
		}
	};

	@Override
	public Set<RuntimeSmartConnector> getSmartConnectors() {
		synchronized (smartConnectors) {
			return new HashSet<>(smartConnectors.values());
		}
	}

	@Override
	public SmartConnector getSmartConnectorById(URI id) {
		synchronized (smartConnectors) {
			return smartConnectors.get(id);
		}
	}

	@Override
	public void addListener(SmartConnectorRegistryListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(SmartConnectorRegistryListener listener) {
		this.listeners.remove(listener);
	}

}
