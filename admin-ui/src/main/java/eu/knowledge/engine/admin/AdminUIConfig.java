package eu.knowledge.engine.admin;

public class AdminUIConfig {

	/**
	 * The key to configure how long (in milliseconds) should the MetadataKB wait
	 * until it tries (for the first time) to ask for all KBs in the network. This
	 * value should probably be higher in distributed mode, to allow the
	 * participants to reach equilibrium with respect to knowledge about each other.
	 */
	public static final String CONF_KEY_INITIAL_METADATA_DELAY = "initial.metadata.delay";

	/**
	 * The key to configure what the delay (in milliseconds) should be between
	 * repeated metadata requests. Sometimes (especially in distributed mode) the
	 * metadata gets corrupted and certain KBs or KIs are (not) available while they
	 * should (not). This makes sure the metadata gets refreshed periodically. Set
	 * this value to {@code 0} disables repeated metadata checks (default).
	 */
	public static final String CONF_KEY_REPEATED_METADATA_DELAY = "repeated.metadata.delay";

}
