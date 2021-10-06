package eu.knowledge.engine.smartconnector.api;

import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.KnowledgeBase;

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

	private final boolean isMeta;

	/**
	 * Create a {@link KnowledgeInteraction}.
	 *
	 * @param act The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 *            It can be read as the 'goal' or 'purpose' of the data exchange and
	 *            whether it has side-effects or not.
	 */
	public KnowledgeInteraction(CommunicativeAct act) {
		this(act, false, false);
	}

	public KnowledgeInteraction(CommunicativeAct act, boolean isMeta, boolean aFullMatchOnly) {
		super();
		this.fullMatchOnly = aFullMatchOnly;
		this.isMeta = isMeta;
		this.act = act;
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
		this(act, isMeta, isMeta);
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
}
