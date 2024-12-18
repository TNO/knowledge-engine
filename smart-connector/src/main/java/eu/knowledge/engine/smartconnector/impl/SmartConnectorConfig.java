package eu.knowledge.engine.smartconnector.impl;

public class SmartConnectorConfig {

	/**
	 * Key to set whether this KER should strictly validate whether outgoing
	 * bindings are compatible with incoming bindings.
	 */
	public static final String CONF_KEY_VALIDATE_OUTGOING_BINDINGS_WRT_INCOMING_BINDINGS = "sc.validate.outgoing.bindings.wrt.incoming.bindings";

	/**
	 * Key to configure the hostname of the machine this Knowledge Engine Runtime
	 * (KER) runs on.
	 * 
	 * @deprecated Replaced by
	 *             {@link SmartConnectorConfig#CONF_KEY_KE_RUNTIME_EXPOSED_URL}
	 */
	@Deprecated
	public static final String CONF_KEY_KE_RUNTIME_HOSTNAME = "ke.runtime.hostname";

	/**
	 * Key to configure the URL of the Knowledge Directory where this KER can find
	 * other KERs in the network. Note that overriding this configuration property
	 * will run this KER in distributed mode.
	 */
	public static final String CONF_KEY_KD_URL = "kd.url";

	/**
	 * Key to configure the time in seconds the SCs in this KER wait for a HTTP
	 * connection response from another KER. Only used in distributed mode.
	 */
	public static final String CONF_KEY_KE_HTTP_TIMEOUT = "ke.http.timeout";

	/**
	 * Key to configure the how many seconds the MessageRouter should wait for
	 * ANSWER/REACT Message when sending a ASK/POST Message? 0 means wait forever
	 * (useful when working with a human KB).
	 */
	public static final String CONF_KEY_KE_KB_WAIT_TIMEOUT = "ke.kb.wait.timeout";

	/**
	 * Key to configure the URL that is advertised to other KERs. Other KERs can use
	 * this URL to reach this KER. Note that this configuration property is only
	 * used in distributed mode.
	 */
	public static final String CONF_KEY_KE_RUNTIME_EXPOSED_URL = "ke.runtime.exposed.url";

	/**
	 * Key to configure the port at which the KER's peer-to-peer communication
	 * should happen. Only used in distributed mode.
	 */
	public static final String CONF_KEY_KE_RUNTIME_PORT = "ke.runtime.port";
}
