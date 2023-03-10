package eu.knowledge.engine.smartconnector.spi;

import eu.knowledge.engine.smartconnector.api.KnowledgeBase;
import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.api.SmartConnectorProvider;
import eu.knowledge.engine.smartconnector.impl.SmartConnectorImpl;

public class SmartConnectorV2Provider implements SmartConnectorProvider {

	@Override
	public SmartConnector create(KnowledgeBase kb) {
		return new SmartConnectorImpl(kb);
	}

}
