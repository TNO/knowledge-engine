package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import java.net.URI;
import java.util.Set;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorImpl;

public interface LocalSmartConnectorRegistry {

	void register(SmartConnectorImpl smartConnector);

	void unregister(SmartConnectorImpl smartConnector);

	Set<SmartConnectorImpl> getSmartConnectors();

	SmartConnector getSmartConnectorById(URI id);

	void addListener(SmartConnectorRegistryListener listener);

	void removeListener(SmartConnectorRegistryListener listener);

}