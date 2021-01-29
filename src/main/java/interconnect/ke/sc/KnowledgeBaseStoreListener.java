package interconnect.ke.sc;

public interface KnowledgeBaseStoreListener {

	void knowledgeInteractionRegistered(KnowledgeInteractionInfo ki);

	void knowledgeInteractionUnregistered(KnowledgeInteractionInfo ki);

}
