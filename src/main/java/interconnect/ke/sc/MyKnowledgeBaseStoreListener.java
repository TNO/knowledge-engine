package interconnect.ke.sc;

public interface MyKnowledgeBaseStoreListener {

	void knowledgeInteractionRegistered(MyKnowledgeInteractionInfo ki);

	void knowledgeInteractionUnregistered(MyKnowledgeInteractionInfo ki);

}
