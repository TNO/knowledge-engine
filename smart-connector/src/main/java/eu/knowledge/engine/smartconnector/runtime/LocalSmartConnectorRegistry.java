package eu.knowledge.engine.smartconnector.runtime;

import java.net.URI;
import java.util.Set;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.impl.RuntimeSmartConnector;

public interface LocalSmartConnectorRegistry {

	void register(RuntimeSmartConnector smartConnector);

	void unregister(RuntimeSmartConnector smartConnector);

	Set<RuntimeSmartConnector> getSmartConnectors();

	SmartConnector getSmartConnectorById(URI id);

	void addListener(SmartConnectorRegistryListener listener);

	void removeListener(SmartConnectorRegistryListener listener);

}