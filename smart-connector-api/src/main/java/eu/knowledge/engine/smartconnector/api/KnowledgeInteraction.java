package eu.knowledge.engine.smartconnector.api;


/**
 * A {@link KnowledgeInteraction} represents an agreement about the exchange of
 * knowledge between the {@link SmartConnectorImpl} and the
 * {@link KnowledgeBase}. It expresses the 'shape' of knowledge that a
 * {@link KnowledgeBase} asks from, or can provide to its
 * {@link SmartConnectorImpl}.
 */
public abstract class KnowledgeInteraction {

	/**
	 * The {@link CommunicativeAct} of this {@link KnowledgeInteraction}, expressing
	 * the intent/purpose or goal of this interaction and whether it has
	 * side-effects.
	 */
	private final CommunicativeAct anAct;

	/**
	 * When executing this knowledge interaction, should the meta knowledge
	 * interactions of the smart connectors be taken into account. This can reduce
	 * performance considerably, because meta KIs have graph patterns that match on
	 * many other graph patterns.
	 */
	private final boolean includeMetaKIs;

	/**
	 * {@code true} if this Knowledge Interaction is used for internal knowledge
	 * engine communication.
	 */
	private final boolean isMeta;

	/**
	 * Optionally configure the matching strategy (which has a large impact on the
	 * performance) to a different level than the one configured at the smart
	 * connector level.
	 * 
	 * For example, this configuration option is used to set the matching process on
	 * all internal (meta) KIs to {@link MatchStrategy#ENTRY_LEVEL}, because we can
	 * make certain assumptions to increase the performance.
	 */
	private MatchStrategy matchStrategy = null;

	protected final String name;

	/**
	 * Create a {@link KnowledgeInteraction}.
	 *
	 * @param act The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 *            It can be read as the 'goal' or 'purpose' of the data exchange and
	 *            whether it has side-effects or not.
	 */
	public KnowledgeInteraction(CommunicativeAct act) {
		this(act, null, false, false, null);
	}

	public KnowledgeInteraction(CommunicativeAct act, boolean isMeta) {
		this(act, null, isMeta, false, null);
	}

	public KnowledgeInteraction(CommunicativeAct act, boolean isMeta, boolean anIncludeMetaKIs) {
		this(act, null, isMeta, anIncludeMetaKIs, null);
	}

	/**
	 * Create a {@link KnowledgeInteraction}.
	 *
	 * @param act              The {@link CommunicativeAct} of this
	 *                         {@link KnowledgeInteraction}. It can be read as the
	 *                         'goal' or 'purpose' of the data exchange and whether
	 *                         it has side-effects or not.
	 * @param name             An optional name for the KI (that also influences and
	 *                         stabilize the URI).
	 * @param isMeta           Whether or not this knowledge interaction contains
	 *                         metadata about the knowledge base itself.
	 * @param anIncludeMetaKIs When processing this knowledge interaction, do we
	 *                         need to include the meta KIs of the SCs within the
	 *                         network? This needs to be set to {@code true} if this
	 *                         KI relies on exchanging KE metadata.
	 * @param aMatchConfig     Optionally configure the match strategy that will be
	 *                         used when executing this KI. If set to {@code null}
	 *                         the match strategy configured at the SC level will be
	 *                         used.
	 */
	public KnowledgeInteraction(CommunicativeAct act, String name, boolean isMeta, boolean anIncludeMetaKIs,
			MatchStrategy aMatchConfig) {
		this.validateName(name);
		this.anAct = act;
		this.name = name;
		this.isMeta = isMeta;
		this.includeMetaKIs = anIncludeMetaKIs;
		this.matchStrategy = aMatchConfig;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * @return The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 */
	public CommunicativeAct getAct() {
		return this.anAct;
	}

	public boolean isMeta() {
		return this.isMeta;
	}

	public boolean includeMetaKIs() {
		return this.includeMetaKIs;
	}

	public MatchStrategy getMatchStrategy() {
		return this.matchStrategy;
	}

	/**
	 * Throws an exception if {@code name} does not conform to the requirements of
	 * knowledge interaction names.
	 * 
	 * @param name
	 */
	private void validateName(String name) {
		if (name != null && !name.matches("[a-zA-Z0-9-]*")) {
			throw new IllegalArgumentException(
					"Knowledge Interaction names can only contain alphanumeric characters and hyphens.");
		}
	}
}
