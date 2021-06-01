package eu.interconnectproject.knowledge_engine.smartconnector.spi;

import eu.interconnectproject.knowledge_engine.smartconnector.api.KnowledgeBase;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnectorProvider;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.SmartConnectorImpl;

public class SmartConnectorV2Provider implements SmartConnectorProvider {

	@Override
	public SmartConnector create(KnowledgeBase kb) {
		return new SmartConnectorImpl(kb, true);
	}

}
