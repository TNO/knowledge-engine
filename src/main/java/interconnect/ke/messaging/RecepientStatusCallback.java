package interconnect.ke.messaging;

public interface RecepientStatusCallback {

	void delivered(KnowledgeMessage message);

	void deliveryFailed(KnowledgeMessage message);

}
