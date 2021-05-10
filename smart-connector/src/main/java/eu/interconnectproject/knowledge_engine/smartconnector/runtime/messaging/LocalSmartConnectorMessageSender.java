package eu.interconnectproject.knowledge_engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ErrorMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.KnowledgeMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.SmartConnectorEndpoint;
import eu.interconnectproject.knowledge_engine.smartconnector.runtime.KeRuntime;

/**
 * This class is responsible for sending messages to a single local Smart
 * Connector.
 */
public class LocalSmartConnectorMessageSender implements SmartConnectorMessageSender {

	public static Logger LOG = LoggerFactory.getLogger(LocalSmartConnectorMessageSender.class);

	private final SmartConnectorEndpoint endpoint;

	public LocalSmartConnectorMessageSender(SmartConnectorEndpoint sce) {
		this.endpoint = sce;
	}

	public URI getKnowledgeBaseId() {
		return this.endpoint.getKnowledgeBaseId();
	}

	@Override
	public void send(KnowledgeMessage message) throws IOException {
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

}
