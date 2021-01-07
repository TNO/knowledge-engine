package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.sc.SmartConnectorImpl;

/**
 * A {@link KnowledgeInteraction} represents an agreement about the exchange of
 * knowledge between the {@link SmartConnectorImpl} and the {@link KnowledgeBase}.
 * It expresses the 'shape' of knowledge that a {@link KnowledgeBase} asks from,
 * or can provide to its {@link SmartConnectorImpl}.
 */
public abstract class KnowledgeInteraction {

	/**
	 * The {@link CommunicativeAct} of this {@link KnowledgeInteraction}, expressing
	 * the intent/purpose or goal of this interaction and whether it has
	 * side-effects.
	 */
	private final CommunicativeAct act;

	/**
	 * Whether or not this knowledge interaction contains metadata about the
	 * knowledge base itself.
	 */
	private final boolean isMeta;

	/**
	 * Create a {@link KnowledgeInteraction}.
	 * 
	 * @param act The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 *            It can be read as the 'goal' or 'purpose' of the data exchange and
	 *            whether it has side-effects or not.
	 */
	public KnowledgeInteraction(CommunicativeAct act) {
		super();
		this.act = act;
		this.isMeta = false;
	}

	/**
	 * Create a {@link KnowledgeInteraction}.
	 *
	 * @param act The {@link CommunicativeAct} of this
	 *            {@link KnowledgeInteraction}. It can be read as the 'goal' or
	 *            'purpose' of the data exchange and whether it has side-effects
	 *            or not.
	 * @param isMeta  Whether or not this knowledge interaction contains metadata
	 * about the knowledge base itself. 
	 */
	public KnowledgeInteraction(CommunicativeAct act, boolean isMeta) {
		super();
		this.act = act;
		this.isMeta = isMeta;
	}

	/**
	 * @return The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 */
	public CommunicativeAct getAct() {
		return act;
	}

	public boolean getIsMeta() {
		return this.isMeta;
	}
}
