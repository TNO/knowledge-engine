package eu.interconnectproject.knowledge_engine.smartconnector.impl;

public interface KnowledgeBaseStoreListener {

	void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki);

	void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki);

}
