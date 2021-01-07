package interconnect.ke.runtime;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.runtime.LocalSmartConnectorRegistry;
import interconnect.ke.api.runtime.SmartConnectorRegistryListener;

/**
 * Singleton object that keeps a reference to every SmartConnector object in
 * this JVM.
 */
public class LocalSmartConnectorRegistryImpl implements LocalSmartConnectorRegistry {

	private Map<URI, SmartConnector> smartConnectors = new HashMap<>();
	private List<SmartConnectorRegistryListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Constructor may only be called by {@link Runtime}
	 */
	LocalSmartConnectorRegistryImpl() {
	}

	@Override
	public void register(SmartConnector smartConnector) {
		synchronized (smartConnectors) {
			if (smartConnectors.containsKey(smartConnector.getEndpoint().getKnowledgeBaseId())) {
				throw new IllegalArgumentException("There already is a smart connector registered with ID "
						+ smartConnector.getEndpoint().getKnowledgeBaseId());
			}
			smartConnectors.put(smartConnector.getEndpoint().getKnowledgeBaseId(), smartConnector);
		}
		for (SmartConnectorRegistryListener l : listeners) {
			l.smartConnectorAdded(smartConnector);
		}
	};

	@Override
	public void unregister(SmartConnector smartConnector) {
		synchronized (smartConnectors) {
			smartConnectors.remove(smartConnector.getEndpoint().getKnowledgeBaseId());
		}
		for (SmartConnectorRegistryListener l : listeners) {
			l.smartConnectorRemoved(smartConnector);
		}
	};

	@Override
	public Set<SmartConnector> getSmartConnectors() {
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
