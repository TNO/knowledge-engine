package eu.knowledge.engine.smartconnector.impl;

import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.SmartConnector;

public class SmartConnectorBuilder {

	private final KnowledgeBase knowledgeBase;

	private SmartConnectorBuilder(KnowledgeBase knowledgeBase) {
		this.knowledgeBase = knowledgeBase;
	}

	public SmartConnector create() {
		return new SmartConnectorImpl(this.knowledgeBase);
	}

	public static SmartConnectorBuilder newSmartConnector(KnowledgeBase knowledgeBase) {
		return new SmartConnectorBuilder(knowledgeBase);
	}

}
