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

		checkNull(knowledgeBase);

		return new SmartConnectorBuilder(knowledgeBase);
	}

	private static void checkNull(KnowledgeBase knowledgeBase) {
		String message = "The KB ";
		boolean allNonNull = true;
		if (knowledgeBase.getKnowledgeBaseId() == null) {
			allNonNull = false;
			message += "id";
		} else if (knowledgeBase.getKnowledgeBaseName() == null) {
			allNonNull = false;
			message += "name";
		} else if (knowledgeBase.getKnowledgeBaseDescription() == null) {
			allNonNull = false;
			message += "description";
		}

		message += " should be non-null.";

		if (!allNonNull)
			throw new NullPointerException(message);
	}

}
