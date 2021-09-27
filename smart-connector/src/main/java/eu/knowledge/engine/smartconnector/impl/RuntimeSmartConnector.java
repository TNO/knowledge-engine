package eu.knowledge.engine.smartconnector.impl;

import java.net.URI;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.messaging.SmartConnectorEndpoint;

public interface RuntimeSmartConnector extends SmartConnector {

	URI getKnowledgeBaseId();

	SmartConnectorEndpoint getSmartConnectorEndpoint();

}
