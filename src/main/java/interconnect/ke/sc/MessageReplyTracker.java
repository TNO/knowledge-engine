package interconnect.ke.sc;

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

/**
 * This class can be used to send {@link AskMessage}s or {@link PostMessage}s
 * towards other SmartConnectors. This class will immedately return a
 * {@link CompletableFuture}, and wait for the corresponding
 * {@link AnswerMessage} or {@link ReactMessage} to arrive, and will notify the
 * original sender of the message via the {@link CompletableFuture} that the
 * reply has arrived.
 */
public class MessageReplyTracker {

	private final static Logger LOG = LoggerFactory.getLogger(MessageReplyTracker.class);

	private MessageDispatcherEndpoint messageDispatcherEndpoint;

	private Map<UUID, CompletableFuture<AnswerMessage>> openAskMessages = new ConcurrentHashMap<>();
	private Map<UUID, CompletableFuture<ReactMessage>> openPostMessages = new ConcurrentHashMap<>();

	public MessageReplyTracker(MessageDispatcherEndpoint messageDispatcherEndpoint) {
		this.messageDispatcherEndpoint = messageDispatcherEndpoint;
	}

	/**
	 * Send an {@link AskMessage}. This method should be called by an internal class
	 * of the SmartConnector. The message will be sent via the
	 * {@link MessageDispatcherEndpoint} provided in the constructor.
	 * 
	 * @param askMessage {@link AskMessage} to be sent.
	 * @return A {@link CompletableFuture} that will be notified once the reply has
	 *         arrived.
	 */
	public CompletableFuture<AnswerMessage> sendAskMessage(AskMessage askMessage) {
		CompletableFuture<AnswerMessage> future = new CompletableFuture<AnswerMessage>();
		openAskMessages.put(askMessage.getMessageId(), future);
		messageDispatcherEndpoint.send(askMessage, null);
		return future;
	}

	/**
	 * Send a {@link PostMessage}. This method should be called by an internal class
	 * of the SmartConnector. The message will be sent via the
	 * {@link MessageDispatcherEndpoint} provided in the constructor.
	 * 
	 * @param postMessage {@link PostMessage} to be sent.
	 * @return A {@link CompletableFuture} that will be notified once the reply has
	 *         arrived.
	 */
	public CompletableFuture<ReactMessage> sendPostMessage(PostMessage postMessage) {
		CompletableFuture<ReactMessage> future = new CompletableFuture<ReactMessage>();
		openPostMessages.put(postMessage.getMessageId(), future);
		messageDispatcherEndpoint.send(postMessage, null);
		return future;
	}

	/**
	 * Handle the arrival of an {@link AnswerMessage}, which is a reply on an
	 * {@link AskMessage} that was originally sent out through this class. This
	 * method should indirectly be called by the MessageDispatcher.
	 * 
	 * @param answerMessage message to handle
	 */
	public void handleAnswerMessage(AnswerMessage answerMessage) {
		CompletableFuture<AnswerMessage> future = openAskMessages.remove(answerMessage.getReplyToAskMessage());
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
	public void handleReactMessage(ReactMessage reactMessage) {
		CompletableFuture<ReactMessage> future = openPostMessages.remove(reactMessage.getReplyToPostMessage());
		if (future == null) {
			LOG.warn("I received a reply for a PostMessage with ID " + reactMessage.getReplyToPostMessage()
					+ ", but I don't remember sending a message with that ID");
		} else {
			future.complete(reactMessage);
		}
	}

}
