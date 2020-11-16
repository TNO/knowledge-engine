package interconnect.ke.api;

public abstract class KnowledgeInteraction {

	private final CommunicativeAct act;

	public KnowledgeInteraction(CommunicativeAct act) {
		super();
		this.act = act;
	}

	public CommunicativeAct getAct() {
		return act;
	}
}
