package eu.knowledge.engine.smartconnector.api;

import org.apache.jena.ext.xerces.impl.xpath.regex.RegularExpression;

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
	private final CommunicativeAct act;

	private final boolean fullMatchOnly;

	/**
	 * {@code true} if this Knowledge Interaction is used for internal knowledge
	 * engine communication.
	 */
	private final boolean isMeta;

	protected final String name;

	/**
	 * Create a {@link KnowledgeInteraction}.
	 *
	 * @param act The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 *            It can be read as the 'goal' or 'purpose' of the data exchange and
	 *            whether it has side-effects or not.
	 */
	public KnowledgeInteraction(CommunicativeAct act) {
		this(act, null, false, false);
	}

	/**
	 * Create a {@link KnowledgeInteraction}.
	 *
	 * @param act    The {@link CommunicativeAct} of this
	 *               {@link KnowledgeInteraction}. It can be read as the 'goal' or
	 *               'purpose' of the data exchange and whether it has side-effects
	 *               or not.
	 * @param isMeta Whether or not this knowledge interaction contains metadata
	 *               about the knowledge base itself.
	 */
	public KnowledgeInteraction(CommunicativeAct act, boolean isMeta) {
		this(act, null, isMeta, false);
	}

	public KnowledgeInteraction(CommunicativeAct act, boolean isMeta, boolean aFullMatchOnly) {
		this(act, null, isMeta, aFullMatchOnly);
	}

	public KnowledgeInteraction(CommunicativeAct act, String name, boolean isMeta, boolean aFullMatchOnly) {
		this.validateName(name);
		this.act = act;
		this.name = name;
		this.isMeta = isMeta;
		this.fullMatchOnly = aFullMatchOnly;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * @return The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 */
	public CommunicativeAct getAct() {
		return this.act;
	}

	public boolean isMeta() {
		return this.isMeta;
	}

	public boolean fullMatchOnly() {
		return this.fullMatchOnly;
	}

	/**
	 * Throws an exception if {@code name} does not conform to the requirements of
	 * knowledge interaction names.
	 * 
	 * @param name
	 */
	private void validateName(String name) {
		if (name != null && !name.matches("[a-zA-Z0-9-]*")) {
			throw new IllegalArgumentException("Knowledge Interaction names can only contain alphanumeric characters and hyphens.");
		}
	}
}
