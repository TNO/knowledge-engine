package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;

import eu.interconnectproject.knowledge_engine.smartconnector.api.SmartConnector;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.SmartConnectorEndpoint;

public interface RuntimeSmartConnector extends SmartConnector {

	URI getKnowledgeBaseId();

	SmartConnectorEndpoint getSmartConnectorEndpoint();

}
