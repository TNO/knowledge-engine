package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import eu.knowledge.engine.smartconnector.api.SmartConnector;
import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.MessageDispatcherEndpoint;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;

/**
 * The MessageRouter is the gateway for the {@link SmartConnector} towards the
 * MessageDispatcher. It provides two functions:
 * <ul>
 * <li>It creates a CompletableFuture for outgoing, proactive messages
 * (AskMessage and PostMessage) so the sending component gets notified one the
 * reply has been received.</li>
 * <li>Incoming messages, for which this {@link SmartConnector} needs to create
 * a reply, are forwarded to the right internal component. For normal messages
 * this is the {@link InteractionProcessor}, for meta data related
 * messages this is the {@link MetaKnowledgeBase}.
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
	 * Register the {@link MetaKnowledgeBase} so it can receive incoming messages
	 * for which a reply needs to be created.
	 *
	 * @param metaKnowledgeBase
	 */
	void registerMetaKnowledgeBase(MetaKnowledgeBase metaKnowledgeBase);

	void registerInteractionProcessor(InteractionProcessor interactionProcessor);

}