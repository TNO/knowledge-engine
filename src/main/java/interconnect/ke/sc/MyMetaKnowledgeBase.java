package interconnect.ke.sc;

import java.net.URI;

import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

public interface MyMetaKnowledgeBase {

	AnswerMessage processAskFromMessageRouter(AskMessage askMessage);

	ReactMessage processPostFromMessageRouter(PostMessage postMessage);

	boolean isMetaKnowledgeInteraction(URI id);

}
