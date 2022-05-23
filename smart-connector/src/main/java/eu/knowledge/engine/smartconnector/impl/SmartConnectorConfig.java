package eu.knowledge.engine.smartconnector.impl;

public class SmartConnectorConfig {
	public static final String CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS = "SC_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS";

	public static boolean getBoolean(String key, boolean defaultValue) {
		String valueString = System.getenv(key);
		if (valueString == null) {
			return defaultValue;
		} else {
			return Boolean.parseBoolean(valueString);
		}
	}
}
