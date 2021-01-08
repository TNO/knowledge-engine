package interconnect.ke.sc;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public interface MessageRouter {

	void setMessageDispatcherEndpoint(MessageDispatcherEndpoint messageDispatcherEndpoint);

	/**
	 * Send an {@link AskMessage}. This method should be called by an internal class
	 * of the SmartConnector. The message will be sent via the
	 * {@link MessageDispatcherEndpoint} provided in the constructor.
	 *
	 * @param askMessage {@link AskMessage} to be sent.
	 * @return A {@link CompletableFuture} that will be notified once the reply has
	 *         arrived.
	 * @throws IOException
	 */
	CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) throws IOException;

	/**
	 * Send a {@link PostMessage}. This method should be called by an internal class
	 * of the SmartConnector. The message will be sent via the
	 * {@link MessageDispatcherEndpoint} provided in the constructor.
	 *
	 * @param postMessage {@link PostMessage} to be sent.
	 * @return A {@link CompletableFuture} that will be notified once the reply has
	 *         arrived.
	 * @throws IOException
	 */
	CompletableFuture<ReactMessage> sendPostMessage(PostMessage postMessage) throws IOException;

	void registerMetaKnowledgeBase(MyMetaKnowledgeBase metaKnowledgeBase);

	void registerInteractionProcessor(ProactiveInteractionProcessor interactionProcessor);

}