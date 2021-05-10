package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;

public abstract interface SmartConnectorMessageSender {

	void send(KnowledgeMessage message) throws IOException;

}
