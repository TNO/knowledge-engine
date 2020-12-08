package interconnect.ke.runtime;

import interconnect.ke.api.SmartConnector;

/**
 * Singleton object that keeps a reference to every SmartConnector object in
 * this JVM.
 */
public class SmartConnectorRegistry {

	public static SmartConnectorRegistry instance;

	private SmartConnectorRegistry() {

	}

	public static SmartConnectorRegistry getInstance() {
		if (instance == null) {
			instance = new SmartConnectorRegistry();
		}
		return instance;
	}

	public void register(SmartConnector smartConnector) {

	};

	public void unregister(SmartConnector smartConnector) {

	};

}
