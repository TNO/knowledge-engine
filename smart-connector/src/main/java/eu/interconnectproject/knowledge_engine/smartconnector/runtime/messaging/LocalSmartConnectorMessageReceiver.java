package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.MessageDispatcherEndpoint;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.SmartConnectorEndpoint;

/**
 * This class is responsible for receiving messages from a single local Smart
 * Connector.
 */
public class LocalSmartConnectorMessageReceiver implements MessageDispatcherEndpoint {

	private final DistributedMessageDispatcher messageDispatcher;
	private final SmartConnectorEndpoint endpoint;

	public LocalSmartConnectorMessageReceiver(DistributedMessageDispatcher messageDispatcher,
			SmartConnectorEndpoint endpoint) {
		this.messageDispatcher = messageDispatcher;
		this.endpoint = endpoint;
	}

	public void start() {
		this.endpoint.setMessageDispatcher(this);
	}

	public void stop() {
		this.endpoint.unsetMessageDispatcher();
	}

	/**
	 * This method is called by the local Smart Connector.
	 */
	@Override
	public void send(KnowledgeMessage message) throws IOException {
		assert message.getFromKnowledgeBase()
				.equals(this.endpoint.getKnowledgeBaseId()) : "the fromKnowledgeBaseId should be mine, but isn't.";
		messageDispatcher.send(message);
	}

}
