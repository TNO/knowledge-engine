package interconnect.ke.messaging;

import java.net.URI;

public interface SmartConnectorEndpoint {

	URI getKnowledgeBaseId();

	void handleAskMessage(AskMessage message);

	void handleAnswerMessage(AnswerMessage message);

	void handlePostMessage(PostMessage message);

	void handleReactMessage(ReactMessage message);

}
