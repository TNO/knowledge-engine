package interconnect.ke.messaging;

public interface MessageDispatcherEndpoint {

	void send(KnowledgeMessage message, RecepientStatusCallback callback);

}
