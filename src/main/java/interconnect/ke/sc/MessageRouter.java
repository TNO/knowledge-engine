package interconnect.ke.sc;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import interconnect.ke.api.SmartConnector;
import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

/**
 * The MessageRouter is the gateway for the {@link SmartConnector} towards the
 * MessageDispatcher. It provides two functions:
 * <ul>
 * <li>It creates a CompletableFuture for outgoing, proactive messages
 * (AskMessage and PostMessage) so the sending component gets notified one the
 * reply has been received.</li>
 * <li>Incoming messages, for which this {@link SmartConnector} needs to create
 * a reply, are forwarded to the right internal component. For normal messages
 * this is the {@link ProactiveInteractionProcessor}, for meta data related
 * messages this is the {@link MyMetaKnowledgeBase}.
 * </ul>
 */
public interface MessageRouter {

	/**
	 * Send an {@link AskMessage}. This method should be called by an internal class
	 * of the SmartConnector. The message will be sent to the
	 * {@link MessageDispatcherEndpoint}.
	 *
	 * @param askMessage {@link AskMessage} to be sent.
	 * @return A {@link CompletableFuture} that will be completed once the reply has
	 *         arrived.
	 * @throws IOException
	 */
	CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) throws IOException;

	/**
	 * Send a {@link PostMessage}. This method should be called by an internal class
	 * of the SmartConnector. The message will be sent to the
	 * {@link MessageDispatcherEndpoint}.
	 *
	 * @param postMessage {@link PostMessage} to be sent.
	 * @return A {@link CompletableFuture} that will be completed once the reply has
	 *         arrived.
	 * @throws IOException
	 */
	CompletableFuture<ReactMessage> sendPostMessage(PostMessage postMessage) throws IOException;

	/**
	 * Register the {@link MyMetaKnowledgeBase} so it can receive incoming messages
	 * for which a reply needs to be created.
	 *
	 * @param metaKnowledgeBase
	 */
	void registerMetaKnowledgeBase(MyMetaKnowledgeBase metaKnowledgeBase);

	void registerInteractionProcessor(InteractionProcessor interactionProcessor);

}