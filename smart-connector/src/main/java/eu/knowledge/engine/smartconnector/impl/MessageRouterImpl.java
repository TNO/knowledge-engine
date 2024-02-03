package eu.knowledge.engine.smartconnector.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import eu.knowledge.engine.smartconnector.messaging.AnswerMessage;
import eu.knowledge.engine.smartconnector.messaging.AskMessage;
import eu.knowledge.engine.smartconnector.messaging.ErrorMessage;
import eu.knowledge.engine.smartconnector.messaging.MessageDispatcherEndpoint;
import eu.knowledge.engine.smartconnector.messaging.PostMessage;
import eu.knowledge.engine.smartconnector.messaging.ReactMessage;
import eu.knowledge.engine.smartconnector.messaging.SmartConnectorEndpoint;

public class MessageRouterImpl implements MessageRouter, SmartConnectorEndpoint {

	private final Logger LOG;

	/**
	 * In principle the open ask/post messages mappings should not have a max
	 * capacity, but to prevent memory leaks we cap them at the number below.
	 */
	private static final int MAX_ENTRIES = 5000;

	private static final int WAIT_TIMEOUT = 30;

	private final SmartConnectorImpl smartConnector;
	private final Map<UUID, CompletableFuture<AnswerMessage>> openAskMessages = Collections
			.synchronizedMap(new LinkedHashMap<UUID, CompletableFuture<AnswerMessage>>() {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<UUID, CompletableFuture<AnswerMessage>> eldest) {
					if (size() > MAX_ENTRIES) {
						eldest.getValue().completeExceptionally(new Exception("There should not be " + MAX_ENTRIES
								+ " open AskMessages. Oldest message with id " + eldest.getKey() + " removed."));
						return true;
					} else {
						return false;
					}
				}
			});
	private final Map<UUID, CompletableFuture<ReactMessage>> openPostMessages = Collections
			.synchronizedMap(new LinkedHashMap<UUID, CompletableFuture<ReactMessage>>() {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<UUID, CompletableFuture<ReactMessage>> eldest) {
					if (size() > MAX_ENTRIES) {
						eldest.getValue().completeExceptionally(new Exception("There should not be " + MAX_ENTRIES
								+ " open PostMessages. Oldest message with id " + eldest.getKey() + " removed."));
						return true;
					} else {
						return false;
					}
				}
			});

	private MessageDispatcherEndpoint messageDispatcherEndpoint = null;

	// TODO remove metaknowledgebase if it is no longer needed (since the
	// interaction processor is now fully responsible for message handling).
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

		// wait maximally WAIT_TIMEOUT for a return message.
		future.orTimeout(WAIT_TIMEOUT, TimeUnit.SECONDS).whenComplete((m, e) -> {
			this.openAskMessages.remove(askMessage.getMessageId());
		});

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

		// wait maximally WAIT_TIMEOUT for a return message.
		future.orTimeout(WAIT_TIMEOUT, TimeUnit.SECONDS).whenComplete((m, e) -> {
			this.openAskMessages.remove(postMessage.getMessageId());
		});

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
		MessageDispatcherEndpoint messageDispatcher = this.messageDispatcherEndpoint;
		if (this.metaKnowledgeBase == null || this.interactionProcessor == null) {
			this.LOG.warn(
					"Received message befor the MessageBroker is connected to the MetaKnowledgeBase or InteractionProcessor, ignoring message");
		} else {

			CompletableFuture<AnswerMessage> replyFuture = this.interactionProcessor
					.processAskFromMessageRouter(message);

			replyFuture.thenAccept(reply -> {
				try {
					messageDispatcher.send(reply);
				} catch (Throwable e) {
					this.LOG.warn("Could not send reply to message " + message.getMessageId());
					this.LOG.debug("", e);
				}
			}).handle((r, e) -> {

				if (r == null && e != null) {
					LOG.error("An exception has occured while handling Ask Message ", e);
					return null;
				} else {
					return r;
				}
			});
		}
	}

	/**
	 * Handle a new incoming {@link PostMessage} to which we need to reply
	 */
	@Override
	public void handlePostMessage(PostMessage message) {
		MessageDispatcherEndpoint messageDispatcher = this.messageDispatcherEndpoint;
		if (this.metaKnowledgeBase == null || this.interactionProcessor == null) {
			this.LOG.warn(
					"Received message befor the MessageBroker is connected to the MetaKnowledgeBase or InteractionProcessor, ignoring message");
		} else {

			CompletableFuture<ReactMessage> replyFuture = this.interactionProcessor
					.processPostFromMessageRouter(message);
			replyFuture.thenAccept(reply -> {
				try {
					messageDispatcher.send(reply);
				} catch (Throwable e) {
					this.LOG.warn("Could not send reply to message " + message.getMessageId(), e);
				}
			}).handle((r, e) -> {

				if (r == null && e != null) {
					LOG.error("An exception has occured while handling Post Message ", e);
					return null;
				} else {
					return r;
				}
			});
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
			future.handle((r, e) -> {

				if (r == null) {
					LOG.error("An exception has occured while handling Answer Message ", e);
					return null;
				} else {
					return r;
				}
			});
			LOG.debug("Received AnswerMessage: {}", answerMessage);
			future.complete(answerMessage);
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
			future.handle((r, e) -> {

				if (r == null) {
					LOG.error("An exception has occured while handling React Message ", e);
					return null;
				} else {
					return r;
				}
			});
			LOG.debug("Received ReactMessage: {}", reactMessage);
			future.complete(reactMessage);
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
