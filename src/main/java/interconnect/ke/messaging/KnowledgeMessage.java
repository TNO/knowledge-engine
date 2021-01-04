package interconnect.ke.messaging;

import java.net.URI;
import java.util.UUID;

public abstract class KnowledgeMessage {

	private UUID messageId;
	private URI fromKnowledgeBase;
	private URI fromKnowledgeInteraction;
	private URI toKnowledgeBase;
	private URI toKnowledgeInteraction;

}
