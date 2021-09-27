package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.ErrorMessage;
import eu.knowledge.engine.smartconnector.messaging.KnowledgeMessage;
import eu.knowledge.engine.smartconnector.messaging.MessageDispatcherEndpoint;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;
import eu.knowledge.engine.smartconnector.messaging.SmartConnectorEndpoint;
import eu.knowledge.engine.smartconnector.runtime.KeRuntime;

/**
 * This class is responsible for sending messages to a single local Smart
 * Connector.
 */
public class LocalSmartConnectorConnection implements MessageDispatcherEndpoint {

	public static Logger LOG = LoggerFactory.getLogger(LocalSmartConnectorConnection.class);

	private final SmartConnectorEndpoint endpoint;
	private final MessageDispatcher messageDispatcher;

	public LocalSmartConnectorConnection(MessageDispatcher messageDispatcher, SmartConnectorEndpoint sce) {
		this.messageDispatcher = messageDispatcher;
		this.endpoint = sce;
	}

	public URI getKnowledgeBaseId() {
		return this.endpoint.getKnowledgeBaseId();
	}

	public void deliverToLocalSmartConnector(KnowledgeMessage message) throws IOException {
		assert message.getToKnowledgeBase().equals(this.endpoint.getKnowledgeBaseId()) : "";
		KeRuntime.executorService().execute(() -> {
			try {
				if (message instanceof AnswerMessage) {
					endpoint.handleAnswerMessage((AnswerMessage) message);
				} else if (message instanceof AskMessage) {
					endpoint.handleAskMessage((AskMessage) message);
				} else if (message instanceof PostMessage) {
					endpoint.handlePostMessage((PostMessage) message);
				} else if (message instanceof ReactMessage) {
					endpoint.handleReactMessage((ReactMessage) message);
				} else if (message instanceof ErrorMessage) {
					endpoint.handleErrorMessage((ErrorMessage) message);
				} else {
					assert false;
				}
			} catch (Throwable t) {
				LOG.error("Error occured while processing message by Smart Connector.", t);
			}
		});
	}

	public void start() {
		this.endpoint.setMessageDispatcher(this);
	}

	public void stop() {
		this.endpoint.unsetMessageDispatcher();
	}

	/**
	 * This method is called by the local Smart Connector to send a message to
	 * another SmartConnector
	 */
	@Override
	public void send(KnowledgeMessage message) throws IOException {
		assert message.getFromKnowledgeBase()
				.equals(this.endpoint.getKnowledgeBaseId()) : "the fromKnowledgeBaseId should be mine, but isn't.";
		messageDispatcher.sendToLocalOrRemoteSmartConnector(message);
	}

}
