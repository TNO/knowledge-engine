package interconnect.ke.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
		public void send(KnowledgeMessage message) throws IOException {
			assert message.getFromKnowledgeBase().equals(this.endpoint.getKnowledgeBaseId());

			SmartConnectorHandler receiver = JvmOnlyMessageDispatcher.this.handlers.get(message.getToKnowledgeBase());
			if (receiver == null) {
				throw new IOException("There is no KnowledgeBase with ID " + message.getToKnowledgeBase());
			} else {
				Runtime.executorService().execute(() -> {
					if (message instanceof AnswerMessage) {
						receiver.getEndpoint().handleAnswerMessage((AnswerMessage) message);
					} else if (message instanceof AskMessage) {
						receiver.getEndpoint().handleAskMessage((AskMessage) message);
					} else if (message instanceof PostMessage) {
						receiver.getEndpoint().handlePostMessage((PostMessage) message);
					} else if (message instanceof ReactMessage) {
						receiver.getEndpoint().handleReactMessage((ReactMessage) message);
					} else {
						assert false;
					}
				});
			}
		}

		public SmartConnectorEndpoint getEndpoint() {
			return endpoint;
		}
	}

	private Map<URI, SmartConnectorHandler> handlers = new HashMap<>();

	/**
	 * Constructor may only be called by {@link Runtime}
	 */
	JvmOnlyMessageDispatcher() {
		Runtime.localSmartConnectorRegistry().addListener(this);
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

	public void stop() {
		Runtime.localSmartConnectorRegistry().removeListener(this);
	}

}
