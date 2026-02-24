package eu.knowledge.engine.admin;

public class AdminUIConfig {

	/**
	 * The key to configure how long (in milliseconds) should the MetadataKB wait
	 * until it tries to ask for all KBs in the network. This value should probably
	 * be higher in distributed mode, to allow the participants to reach equilibrium
	 * with respect to knowledge about each other.
	 */
	public static final String CONF_KEY_INITIAL_METADATA_DELAY = "initial.metadata.delay";

}
