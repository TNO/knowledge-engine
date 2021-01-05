package interconnect.ke.messaging;

/**
 * The {@link MessageDispatcherEndpoint} can be used for sending messages to
 * other Smart Connectors.
 */
public interface MessageDispatcherEndpoint {

	void send(KnowledgeMessage message, RecepientStatusCallback callback);

}
