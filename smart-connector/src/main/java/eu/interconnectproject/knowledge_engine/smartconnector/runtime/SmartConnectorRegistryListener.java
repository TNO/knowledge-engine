package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorImpl;

public interface SmartConnectorRegistryListener {

	void smartConnectorAdded(SmartConnectorImpl smartConnector);

	void smartConnectorRemoved(SmartConnectorImpl smartConnector);

}
