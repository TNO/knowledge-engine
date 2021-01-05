package interconnect.ke.sc;

/**
 * An {@link OtherKnowledgeBaseStore} is responsible for keeping track of
 * metadata about knowledge bases in the knowledge network. In the MVP, it
 * should poll the network periodically for other {@link KnowledgeBase}s'
 * {@link KnowledgeInteraction}s and their {@link SmartConnector}s' endpoints.
 * 
 * It uses the {@link KnowledgeDirectory} to discover other smart connectors.
 */
public interface OtherKnowledgeBaseStore {
	
}
