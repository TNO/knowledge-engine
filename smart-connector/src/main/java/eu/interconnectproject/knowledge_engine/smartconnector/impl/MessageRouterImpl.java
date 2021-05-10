package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AnswerMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.AskMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ErrorMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.MessageDispatcherEndpoint;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.PostMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.ReactMessage;
import eu.interconnectproject.knowledge_engine.smartconnector.messaging.SmartConnectorEndpoint;

public class MessageRouterImpl implements MessageRouter, SmartConnectorEndpoint {

	private final Logger LOG;

	private final SmartConnectorImpl smartConnector;
	private final Map<UUID, CompletableFuture<AnswerMessage>> openAskMessages = new ConcurrentHashMap<>();
	private final Map<UUID, CompletableFuture<ReactMessage>> openPostMessages = new ConcurrentHashMap<>();

	private MessageDispatcherEndpoint messageDispatcherEndpoint = null;
	private MetaKnowledgeBase metaKnowledgeBase;
	private InteractionProcessor interactionProcessor;

	/** Indicates if we already called myKnowledgeBase.smartConnectorReady(this) */
	private boolean smartConnectorReadyNotified = false;

	public MessageRouterImpl(SmartConnectorImpl smartConnector) {
		this.LOG = smartConnector.getLogger(MessageRouterImpl.class);

		this.smartConnector = smartConnector;
	}

	@Override
	public CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) throws IOException {
		MessageDispatcherEndpoint messageDispatcher = this.messageDispatcherEndpoint;
		if (messageDispatcher == null) {
			throw new IOException("Not connected to MessageDispatcher");
		}
		CompletableFuture<AnswerMessage> future = new CompletableFuture<>();
		this.openAskMessages.put(askMessage.getMessageId(), future);
		messageDispatcher.send(askMessage);

		LOG.debug("Sent AskMessage: {}", askMessage);

		return future;
	}

	@Override
	public CompletableFuture<ReactMessage> sendPostMessage(PostMessage postMessage) throws IOException {
		MessageDispatcherEndpoint messageDispatcher = this.messageDispatcherEndpoint;
		if (messageDispatcher == null) {
			throw new IOException("Not connected to MessageDispatcher");
		}
		CompletableFuture<ReactMessage> future = new CompletableFuture<>();
		this.openPostMessages.put(postMessage.getMessageId(), future);
		messageDispatcher.send(postMessage);
		LOG.debug("Sent PostMessage: {}", postMessage);

		return future;
	}

	/**
	 * Handle a new incoming {@link AskMessage} to which we need to reply
	 */
	@Override
	public void handleAskMessage(AskMessage message) {
		if (this.metaKnowledgeBase == null || this.interactionProcessor == null) {
			this.LOG.warn(
					"Received message befor the MessageBroker is connected to the MetaKnowledgeBase or InteractionProcessor, ignoring message");
		} else {
			if (this.metaKnowledgeBase.isMetaKnowledgeInteraction(message.getToKnowledgeInteraction())) {
				AnswerMessage reply = this.metaKnowledgeBase.processAskFromMessageRouter(message);
				try {
					this.messageDispatcherEndpoint.send(reply);
				} catch (IOException e) {
					this.LOG.warn("Could not send reply to message " + message.getMessageId(), e);
				}
			} else {
				CompletableFuture<AnswerMessage> replyFuture = this.interactionProcessor
						.processAskFromMessageRouter(message);
				replyFuture.thenAccept(reply -> {
					try {
						this.messageDispatcherEndpoint.send(reply);
					} catch (Throwable e) {
						this.LOG.warn("Could not send reply to message " + message.getMessageId(), e);
					}
				});
			}
		}
	}

	/**
	 * Handle a new incoming {@link PostMessage} to which we need to reply
	 */
	@Override
	public void handlePostMessage(PostMessage message) {
		if (this.metaKnowledgeBase == null || this.interactionProcessor == null) {
			this.LOG.warn(
					"Received message befor the MessageBroker is connected to the MetaKnowledgeBase or InteractionProcessor, ignoring message");
		} else {
			if (this.metaKnowledgeBase.isMetaKnowledgeInteraction(message.getToKnowledgeInteraction())) {
				ReactMessage reply = this.metaKnowledgeBase.processPostFromMessageRouter(message);
				try {
					this.messageDispatcherEndpoint.send(reply);
				} catch (IOException e) {
					this.LOG.warn("Could not send reply to message " + message.getMessageId(), e);
				}
			} else {
				CompletableFuture<ReactMessage> replyFuture = this.interactionProcessor
						.processPostFromMessageRouter(message);
				replyFuture.thenAccept(reply -> {
					try {
						this.messageDispatcherEndpoint.send(reply);
					} catch (Throwable e) {
						this.LOG.warn("Could not send reply to message " + message.getMessageId(), e);
					}
				});
			}
		}
	}

	/**
	 * Handle an incoming {@link AnswerMessage} which is a reply to an
	 * {@link AskMessage} we sent out earlier
	 */
	@Override
	public void handleAnswerMessage(AnswerMessage answerMessage) {
		CompletableFuture<AnswerMessage> future = this.openAskMessages.remove(answerMessage.getReplyToAskMessage());
		if (future == null) {
			this.LOG.warn("I received a reply for an AskMessage with ID " + answerMessage.getReplyToAskMessage()
					+ ", but I don't remember sending a message with that ID");
		} else {
			future.complete(answerMessage);
			LOG.debug("Received AnswerMessage: {}", answerMessage);
		}
	}

	/**
	 * Handle an incoming {@link ReactMessage} which is a reply to an
	 * {@link PostMessage} we sent out earlier
	 */
	@Override
	public void handleReactMessage(ReactMessage reactMessage) {
		CompletableFuture<ReactMessage> future = this.openPostMessages.remove(reactMessage.getReplyToPostMessage());
		if (future == null) {
			this.LOG.warn("I received a reply for a PostMessage with ID " + reactMessage.getReplyToPostMessage()
					+ ", but I don't remember sending a message with that ID");
		} else {
			assert reactMessage != null;
			assert future != null;
			future.complete(reactMessage);
			LOG.debug("Received ReactMessage: {}", reactMessage);
		}
	}

	@Override
	public void handleErrorMessage(ErrorMessage message) {
		// TODO Still needs to be implemented!
		throw new UnsupportedOperationException("Handling of ErrorMessages not yet implemented!");
	}

	@Override
	public void registerMetaKnowledgeBase(MetaKnowledgeBase metaKnowledgeBase) {
		assert this.metaKnowledgeBase == null;

		this.metaKnowledgeBase = metaKnowledgeBase;
	}

	@Override
	public void registerInteractionProcessor(InteractionProcessor interactionProcessor) {
		assert this.interactionProcessor == null;
		this.interactionProcessor = interactionProcessor;
	}

	@Override
	public URI getKnowledgeBaseId() {
		return this.smartConnector.getKnowledgeBaseId();
	}

	@Override
	public void setMessageDispatcher(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		assert this.messageDispatcherEndpoint == null;

		this.messageDispatcherEndpoint = messageDispatcherEndpoint;

		if (!this.smartConnectorReadyNotified) {
			this.smartConnector.communicationReady();
			this.smartConnectorReadyNotified = true;
		} else {
			this.smartConnector.communicationRestored();
		}
	}

	@Override
	public void unsetMessageDispatcher() {
		assert this.messageDispatcherEndpoint != null;

		this.messageDispatcherEndpoint = null;

		this.smartConnector.communicationInterrupted();
	}

}
