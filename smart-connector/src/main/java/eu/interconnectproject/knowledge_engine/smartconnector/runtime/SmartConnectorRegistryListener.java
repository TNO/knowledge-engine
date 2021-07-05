package eu.interconnectproject.knowledge_engine.smartconnector.runtime;

import eu.interconnectproject.knowledge_engine.smartconnector.impl.RuntimeSmartConnector;

public interface SmartConnectorRegistryListener {

	void smartConnectorAdded(RuntimeSmartConnector smartConnector);

	void smartConnectorRemoved(RuntimeSmartConnector smartConnector);

}
