package eu.knowledge.engine.smartconnector.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartConnectorConfig {

	/**
	 * The log facility of this class.
	 */
	public static final Logger LOG = LoggerFactory.getLogger(SmartConnectorConfig.class);

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

	/**
	 * Key to configure if a KER should use the EDC functionality or not.
	 */
	public static final String CONF_KEY_KE_RUNTIME_USE_EDC = "ke.runtime.use.edc";

	/**
	 * Key to configure where a KER can reach the protocol API of its own control
	 * plane if using EDC.
	 */
	public static final String CONF_KEY_KE_EDC_PROTOCOL_URL = "ke.edc.protocol.url";

	/**
	 * Key to configure where a KER can reach the management API of its own control
	 * plane if using EDC.
	 */
	public static final String CONF_KEY_KE_EDC_MANAGEMENT_URL = "ke.edc.management.url";

	/**
	 * Key to configure where a KER can reach its data plane control API if using
	 * EDC.
	 */
	public static final String CONF_KEY_KE_EDC_DATAPLANE_CONTROL_URL = "ke.edc.dataplane.control.url";

	/**
	 * Key to configure where a KER can reach its data plane public API if using
	 * EDC.
	 */
	public static final String CONF_KEY_KE_EDC_DATAPLANE_PUBLIC_URL = "ke.edc.dataplane.public.url";

	/**
	 * Key to configure the URL where a KER can do token validation through the
	 * control plane if using EDC.
	 */
	public static final String CONF_KEY_KE_EDC_TOKEN_VALIDATION_ENDPOINT = "ke.edc.token.validation.endpoint";

	/**
	 * Key to configure the default reasoner level (1-5) that is used in the current
	 * KER when no reasoner level is provided by the user. The meaning of each
	 * levels is:
	 * <ul>
	 * <li><b>1</b>: Fastest but least thorough level. Configures the matching
	 * algorithm to {@link MatchStrategy#ENTRY_LEVEL}.</li>
	 * <li><b>2</b>: Faster but not very thorough level. Configures the matching
	 * algorithm to {@link MatchStrategy#NORMAL_LEVEL}.</li>
	 * <li><b>3</b>: Slower but more thorough level. Configures the matching
	 * algorithm to {@link MatchStrategy#ADVANCED_LEVEL}.</li>
	 * <li><b>4</b>: Even slower but even more thorough level. Configures the
	 * matching algorithm to {@link MatchStrategy#ULTRA_LEVEL}.</li>
	 * <li><b>5</b>: Slowest but most thorough level. Configures the matching
	 * algorithm to {@link MatchStrategy#SUPREME_LEVEL}.</li>
	 * </ul>
	 */
	public static final String CONF_KEY_KE_REASONER_LEVEL = "ke.reasoner.level";

	/**
	 * Path to a file that contains the default domain knowledge that will be
	 * included in every smart connector created in this KE Runtime. This domain
	 * knowledge can include both rules and facts, where the latter are encoded as
	 * body-less (or antecedent-less) rules with a single head triple pattern.
	 * 
	 * The syntax used to describe the domain knowledge is the
	 * <a href="https://jena.apache.org/documentation/inference/#RULEsyntax">Apache
	 * Jena Rules specification</a>.
	 */
	public static final String CONF_KEY_KE_DOMAIN_KNOWLEDGE_PATH = "ke.domain.knowledge.path";

	/**
	 * The main thread pool of the Knowledge Engine Runtime that is used to, among
	 * other things, deliver messages.
	 */
	public static final String CONF_KEY_KE_THREADPOOL_SIZE = "ke.threadpool.size";

	/**
	 * Convert the configuration reasoner levels to matching strategies used in the
	 * reasoner code.
	 * 
	 * @param aReasonerLevel The reasoner level configured in a configuration file.
	 * @return The corresponding matching strategy belonging to the given reasoner
	 *         level.
	 */
	public static MatchStrategy toMatchStrategy(int aReasonerLevel) {
		MatchStrategy m = null;

		switch (aReasonerLevel) {
		case 1:
			m = MatchStrategy.ENTRY_LEVEL;
			break;
		case 2:
			m = MatchStrategy.NORMAL_LEVEL;
			break;
		case 3:
			m = MatchStrategy.ADVANCED_LEVEL;
			break;
		case 4:
			m = MatchStrategy.ULTRA_LEVEL;
			break;
		case 5:
			m = MatchStrategy.SUPREME_LEVEL;
			break;
		default:
			LOG.warn(
					"The configured reasoner level should lie between 1 and 5, inclusive and should not be '{}'. Falling back to reasoner level 1.",
					aReasonerLevel);
			m = MatchStrategy.ENTRY_LEVEL;
		}

		return m;
	}
}
