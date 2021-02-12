package eu.interconnectproject.knowledge_engine.smartconnector.messaging;

import java.io.IOException;

/**
 * The {@link MessageDispatcherEndpoint} can be used for sending messages to
 * other Smart Connectors.
 */
public interface MessageDispatcherEndpoint {

	void send(KnowledgeMessage message) throws IOException;

}
