package eu.knowledge.engine.smartconnector.runtime;

import eu.knowledge.engine.smartconnector.impl.RuntimeSmartConnector;

public interface SmartConnectorRegistryListener {

	void smartConnectorAdded(RuntimeSmartConnector smartConnector);

	void smartConnectorRemoved(RuntimeSmartConnector smartConnector);

}
