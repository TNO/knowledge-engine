package interconnect.ke.api.runtime;

import java.net.URI;
import java.util.Set;

import interconnect.ke.api.SmartConnector;

public interface SmartConnectorRegistry {

	void register(SmartConnector smartConnector);

	void unregister(SmartConnector smartConnector);

	Set<SmartConnector> getSmartConnectors();

	SmartConnector getSmartConnectorById(URI id);

	void addListener(SmartConnectorRegistryListener listener);

	void removeListener(SmartConnectorRegistryListener listener);

}