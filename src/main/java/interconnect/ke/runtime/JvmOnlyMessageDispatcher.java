package interconnect.ke.runtime;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.runtime.SmartConnectorRegistryListener;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.KnowledgeMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.messaging.RecepientStatusCallback;
import interconnect.ke.messaging.SmartConnectorEndpoint;

/**
 * This class is responsible for delivering messages between
 * {@link SmartConnector}s. THIS VERSION ONLY WORKS FOR THE JVM ONLY. REPLACE
 * FOR DISTRIBUTED VERSION OF KNOWLEDGE ENGINE. TODO
 */
public class JvmOnlyMessageDispatcher implements SmartConnectorRegistryListener {

	protected static Logger LOG = LoggerFactory.getLogger(JvmOnlyMessageDispatcher.class);

	private class SmartConnectorHandler implements MessageDispatcherEndpoint {

		private SmartConnectorEndpoint endpoint;

		public SmartConnectorHandler(SmartConnectorEndpoint sce) {
			this.endpoint = sce;
		}

		@Override
		public void send(KnowledgeMessage message, RecepientStatusCallback callback) {
			JvmOnlyMessageDispatcher.this.executor.execute(() -> {
				SmartConnectorHandler receiver = JvmOnlyMessageDispatcher.this.handlers
						.get(message.getToKnowledgeBase());
				if (receiver == null) {
					fail(message, callback, "There is no KnowledgeBase with ID " + message.getToKnowledgeBase());
				} else {
					if (message instanceof AnswerMessage) {
						receiver.getEndpoint().handleAnswerMessage((AnswerMessage) message);
					} else if (message instanceof AskMessage) {
						receiver.getEndpoint().handleAskMessage((AskMessage) message);
					} else if (message instanceof PostMessage) {
						receiver.getEndpoint().handlePostMessage((PostMessage) message);
					} else if (message instanceof ReactMessage) {
						receiver.getEndpoint().handleReactMessage((ReactMessage) message);
					}
					if (callback != null) {
						callback.delivered(message);
					}
				}
			});
		}

		private void fail(KnowledgeMessage message, RecepientStatusCallback callback, String error) {
			if (callback != null) {
				// TODO other type of Throwable?
				callback.deliveryFailed(message, new IllegalStateException(error));
			}
			LOG.warn("Could not deliver message from " + message.getFromKnowledgeInteraction() + " to "
					+ message.getToKnowledgeBase() + ": " + error);
		}

		public SmartConnectorEndpoint getEndpoint() {
			return endpoint;
		}
	}

	private Map<URI, SmartConnectorHandler> handlers = new HashMap<>();
	private ExecutorService executor = Executors.newFixedThreadPool(4);

	public JvmOnlyMessageDispatcher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void smartConnectorAdded(SmartConnector smartConnector) {
		// Create a new SmartConnectorHandler and attach it
		SmartConnectorEndpoint endpoint = smartConnector.getEndpoint();
		this.handlers.put(endpoint.getKnowledgeBaseId(), new SmartConnectorHandler(endpoint));
	}

	@Override
	public void smartConnectorRemoved(SmartConnector smartConnector) {
		this.handlers.remove(smartConnector.getEndpoint().getKnowledgeBaseId());
	}

}
