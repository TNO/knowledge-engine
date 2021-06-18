package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.net.URI;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.RuntimeSmartConnector;

public interface LocalSmartConnectorRegistry {

	void register(RuntimeSmartConnector smartConnector);

	void unregister(RuntimeSmartConnector smartConnector);

	Set<RuntimeSmartConnector> getSmartConnectors();

	SmartConnector getSmartConnectorById(URI id);

	void addListener(SmartConnectorRegistryListener listener);

	void removeListener(SmartConnectorRegistryListener listener);

}