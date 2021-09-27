package eu.knowledge.engine.smartconnector.messaging;

import java.net.URI;

public interface SmartConnectorEndpoint {

	URI getKnowledgeBaseId();

	void handleAskMessage(AskMessage message);

	void handleAnswerMessage(AnswerMessage message);

	void handlePostMessage(PostMessage message);

	void handleReactMessage(ReactMessage message);

	void handleErrorMessage(ErrorMessage message);

	void setMessageDispatcher(MessageDispatcherEndpoint messageDispatcherEndpoint);

	void unsetMessageDispatcher();

}
