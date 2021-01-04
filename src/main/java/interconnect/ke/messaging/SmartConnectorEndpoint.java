package interconnect.ke.messaging;

import java.net.URI;

public interface SmartConnectorEndpoint {

	URI getKnowledgeBaseId();

	void setMessageDispatcherEndpoint(MessageDispatcherEndpoint endpoint);

	void unsetMessageDispatcherEndpoint(MessageDispatcherEndpoint endpoint);

	void handleAskMessage(AskMessage message);

	void handleAnswerMessage(AnswerMessage message);

	void handlePostMessage(PostMessage message);

	void handleReactMessage(ReactMessage message);

}
