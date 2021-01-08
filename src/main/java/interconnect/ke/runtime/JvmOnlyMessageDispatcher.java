package interconnect.ke.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.SmartConnector;
import interconnect.ke.api.runtime.LocalSmartConnectorRegistry;
import interconnect.ke.api.runtime.SmartConnectorRegistryListener;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.KnowledgeMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.messaging.SmartConnectorEndpoint;
import interconnect.ke.sc.SmartConnectorImpl;

/**
 * This class is responsible for delivering messages between
 * {@link SmartConnectorImpl}s. Once constructed, it registers itself at the
 * {@link LocalSmartConnectorRegistry} and at all the {@link SmartConnector}s.
 *
 * THIS VERSION ONLY WORKS FOR THE JVM ONLY. REPLACE FOR DISTRIBUTED VERSION OF
 * KNOWLEDGE ENGINE. TODO
 */
public class JvmOnlyMessageDispatcher implements SmartConnectorRegistryListener {

	protected static Logger LOG = LoggerFactory.getLogger(JvmOnlyMessageDispatcher.class);

	private class SmartConnectorHandler implements MessageDispatcherEndpoint {

		private final SmartConnectorEndpoint endpoint;

		public SmartConnectorHandler(SmartConnectorEndpoint sce) {
			this.endpoint = sce;
			this.endpoint.setMessageDispatcher(this);
		}

		@Override
		public void send(KnowledgeMessage message) throws IOException {
			assert message.getFromKnowledgeBase().equals(this.endpoint.getKnowledgeBaseId());

			SmartConnectorHandler receiver = JvmOnlyMessageDispatcher.this.handlers.get(message.getToKnowledgeBase());
			if (receiver == null) {
				throw new IOException("There is no KnowledgeBase with ID " + message.getToKnowledgeBase());
			} else {
				KeRuntime.executorService().execute(() -> {
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
			return this.endpoint;
		}
	}

	private final Map<URI, SmartConnectorHandler> handlers = new HashMap<>();

	/**
	 * Constructor may only be called by {@link KeRuntime}
	 */
	JvmOnlyMessageDispatcher() {
		KeRuntime.localSmartConnectorRegistry().addListener(this);

		// Add all the smart connectors that already existed before we were registered
		// as listener
		for (SmartConnectorImpl sc : KeRuntime.localSmartConnectorRegistry().getSmartConnectors()) {
			this.smartConnectorAdded(sc);
		}
	}

	@Override
	public void smartConnectorAdded(SmartConnectorImpl smartConnector) {
		// Create a new SmartConnectorHandler and attach it
		SmartConnectorEndpoint endpoint = smartConnector.getSmartConnectorEndpoint();
		this.handlers.put(endpoint.getKnowledgeBaseId(), new SmartConnectorHandler(endpoint));
	}

	@Override
	public void smartConnectorRemoved(SmartConnectorImpl smartConnector) {
		this.handlers.remove(smartConnector.getKnowledgeBaseId());
	}

	public void stop() {
		KeRuntime.localSmartConnectorRegistry().removeListener(this);
	}

}
