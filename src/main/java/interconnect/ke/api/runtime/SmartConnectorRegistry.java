package interconnect.ke.api.runtime;

import java.net.URI;
import java.util.Set;

import interconnect.ke.api.SmartConnector;
import interconnect.ke.sc.SmartConnectorImpl;

public interface SmartConnectorRegistry {

	void register(SmartConnectorImpl smartConnector);

	void unregister(SmartConnectorImpl smartConnector);

	Set<SmartConnectorImpl> getSmartConnectors();

	SmartConnector getSmartConnectorById(URI id);

	void addListener(SmartConnectorRegistryListener listener);

	void removeListener(SmartConnectorRegistryListener listener);

}