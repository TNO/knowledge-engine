package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;
import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.SmartConnector;

/**
 * A {@link KnowledgeInteraction} represents an agreement about the exchange of
 * knowledge between the {@link SmartConnector} and the {@link KnowledgeBase}.
 * It expresses the 'shape' of knowledge that a {@link KnowledgeBase} asks from,
 * or can provide to its {@link SmartConnector}.
 */
public abstract class KnowledgeInteraction {

	/**
	 * The {@link CommunicativeAct} of this {@link KnowledgeInteraction}, expressing
	 * the intent/purpose or goal of this interaction and whether it has
	 * side-effects.
	 */
	private final CommunicativeAct act;

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
	}

	/**
	 * @return The {@link CommunicativeAct} of this {@link KnowledgeInteraction}.
	 */
	public CommunicativeAct getAct() {
		return act;
	}
}
