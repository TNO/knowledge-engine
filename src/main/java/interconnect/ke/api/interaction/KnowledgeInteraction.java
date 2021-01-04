package interconnect.ke.api.interaction;

import interconnect.ke.api.CommunicativeAct;

/**
 * A KnowledgeInteraction represents an agreement about the exchange of
 * knowledge between the smart connector and the knowledge base. It can express
 * the 'shape' of knowledge that a knowledge base asks for, or can provide its
 * smart connector with.
 */
public abstract class KnowledgeInteraction {

	/**
	 * The CommunicativeAct of this KnowledgeInteraction, expressing the intent of
	 * this interaction.
	 */
	private final CommunicativeAct act;

	public KnowledgeInteraction(CommunicativeAct act) {
		super();
		this.act = act;
	}

	/**
	 * @return The CommunicativeAct of this KnowledgeInteraction.
	 */
	public CommunicativeAct getAct() {
		return act;
	}
}
