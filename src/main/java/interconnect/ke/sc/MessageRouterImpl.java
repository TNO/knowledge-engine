package interconnect.ke.sc;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.MessageDispatcherEndpoint;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;
import interconnect.ke.messaging.SmartConnectorEndpoint;

/**
 * This class can be used to send {@link AskMessage}s or {@link PostMessage}s
 * towards other SmartConnectors. This class will immedately return a
 * {@link CompletableFuture}, and wait for the corresponding
 * {@link AnswerMessage} or {@link ReactMessage} to arrive, and will notify the
 * original sender of the message via the {@link CompletableFuture} that the
 * reply has arrived.
 */
public class MessageRouterImpl implements MessageRouter, SmartConnectorEndpoint {

	private final static Logger LOG = LoggerFactory.getLogger(MessageRouterImpl.class);

	private final Map<UUID, CompletableFuture<AnswerMessage>> openAskMessages = new ConcurrentHashMap<>();
	private final Map<UUID, CompletableFuture<ReactMessage>> openPostMessages = new ConcurrentHashMap<>();

	private MessageDispatcherEndpoint messageDispatcherEndpoint = null;

	@Override
	public CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) throws IOException {
		CompletableFuture<AnswerMessage> future = new CompletableFuture<>();
		this.openAskMessages.put(askMessage.getMessageId(), future);
		this.messageDispatcherEndpoint.send(askMessage);
		return future;
	}

	@Override
	public CompletableFuture<ReactMessage> sendPostMessage(PostMessage postMessage) throws IOException {
		CompletableFuture<ReactMessage> future = new CompletableFuture<>();
		this.openPostMessages.put(postMessage.getMessageId(), future);
		this.messageDispatcherEndpoint.send(postMessage);
		return future;
	}

	/**
	 * Handle the arrival of an {@link AnswerMessage}, which is a reply on an
	 * {@link AskMessage} that was originally sent out through this class. This
	 * method should indirectly be called by the MessageDispatcher.
	 *
	 * @param answerMessage message to handle
	 */
	@Override
	public void handleAnswerMessage(AnswerMessage answerMessage) {
		CompletableFuture<AnswerMessage> future = this.openAskMessages.remove(answerMessage.getReplyToAskMessage());
		if (future == null) {
			LOG.warn("I received a reply for an AskMessage with ID " + answerMessage.getReplyToAskMessage()
					+ ", but I don't remember sending a message with that ID");
		} else {
			future.complete(answerMessage);
		}
	}

	/**
	 * Handle the arrival of an {@link ReactMessage}, which is a reply on a
	 * {@link PostMessage} that was originally sent out through this class. This
	 * method should indirectly be called by the MessageDispatcher.
	 *
	 * @param reactMessage message to handle
	 */
	@Override
	public void handleReactMessage(ReactMessage reactMessage) {
		CompletableFuture<ReactMessage> future = this.openPostMessages.remove(reactMessage.getReplyToPostMessage());
		if (future == null) {
			LOG.warn("I received a reply for a PostMessage with ID " + reactMessage.getReplyToPostMessage()
					+ ", but I don't remember sending a message with that ID");
		} else {
			future.complete(reactMessage);
		}
	}

	@Override
	public void registerMetaKnowledgeBase(MyMetaKnowledgeBase metaKnowledgeBase) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerInteractionProcessor(ProactiveInteractionProcessor interactionProcessor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMessageDispatcherEndpoint(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		this.messageDispatcherEndpoint = messageDispatcherEndpoint;
	}

	@Override
	public void handleAskMessage(AskMessage message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePostMessage(PostMessage message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMessageDispatcher(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unsetMessageDispatcher() {
		// TODO Auto-generated method stub

	}

	@Override
	public URI getKnowledgeBaseId() {
		// TODO Auto-generated method stub
		return null;
	}

}
