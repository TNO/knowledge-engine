package interconnect.ke.api.runtime;

import interconnect.ke.sc.SmartConnectorImpl;

public interface SmartConnectorRegistryListener {

	void smartConnectorAdded(SmartConnectorImpl smartConnector);

	void smartConnectorRemoved(SmartConnectorImpl smartConnector);

}
