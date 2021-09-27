package eu.knowledge.engine.smartconnector.impl;

public interface KnowledgeBaseStoreListener {

	void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki);

	void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki);
	
	/**
	 * The Knowledge Base has requested the smart connector to stop.
	 */
	void smartConnectorStopping();

}
