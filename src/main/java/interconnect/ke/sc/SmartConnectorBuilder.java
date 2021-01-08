package interconnect.ke.sc;

import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.SmartConnector;

public class SmartConnectorBuilder {

	private final KnowledgeBase knowledgeBase;
	private boolean knowledgeBaseIsThreadSafe = false;

	private SmartConnectorBuilder(KnowledgeBase knowledgeBase) {
		this.knowledgeBase = knowledgeBase;
	}

	public SmartConnectorBuilder knowledgeBaseIsThreadSafe(boolean knowledgeBaseIsThreadSafe) {
		this.knowledgeBaseIsThreadSafe = knowledgeBaseIsThreadSafe;
		return this;
	}

	public SmartConnector create() {
		return new SmartConnectorImpl(this.knowledgeBase, this.knowledgeBaseIsThreadSafe);
	}

	public static SmartConnectorBuilder newSmartConnector(KnowledgeBase knowledgeBase) {
		return new SmartConnectorBuilder(knowledgeBase);
	}

}
