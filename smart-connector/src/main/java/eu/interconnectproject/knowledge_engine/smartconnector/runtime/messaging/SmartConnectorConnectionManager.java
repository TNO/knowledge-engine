package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;

public abstract class SmartConnectorConnectionManager {

	public abstract URI getSmartConnectorId();

	public abstract void sendMessage(KnowledgeMessage message) throws IOException;

}
