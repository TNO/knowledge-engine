package interconnect.ke.sc;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import interconnect.ke.messaging.AnswerMessage;
import interconnect.ke.messaging.AskMessage;
import interconnect.ke.messaging.PostMessage;
import interconnect.ke.messaging.ReactMessage;

/**
 * This class is responsible for:
 * - dereferencing knowledge base URIs into {@link OtherKnowledgeBase} objects. 
 * - answering incoming meta ASK interactions that ask for metaknowledge about
 *   this knowledge base.
 * In short, it is very similar to the {@InteractionProcessor} for the reactive
 * meta-knowledge messages, but for the proactive messages, it is also
 * responsible for parsing the knowledge base IDs into meta knowledge
 * interaction using a convention. This convention is also defined by the class
 * implementing this interface.
 */
public interface MetaKnowledgeBase {
	AnswerMessage processAskFromMessageRouter(AskMessage anAskMessage);
	ReactMessage processPostFromMessageRouter(PostMessage aPostMessage);

	CompletableFuture<OtherKnowledgeBase> getOtherKnowledgeBase(URI knowledgeBaseId);
}
