package interconnect.ke.api.runtime;

import interconnect.ke.api.SmartConnector;

public interface SmartConnectorRegistryListener {

	void smartConnectorAdded(SmartConnector smartConnector);

	void smartConnectorRemoved(SmartConnector smartConnector);

}
