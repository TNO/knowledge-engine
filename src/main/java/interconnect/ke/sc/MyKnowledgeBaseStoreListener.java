package interconnect.ke.sc;

public interface MyKnowledgeBaseStoreListener {

	void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki);

	void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki);

}
