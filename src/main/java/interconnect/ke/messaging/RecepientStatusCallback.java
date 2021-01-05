package interconnect.ke.messaging;

public interface RecepientStatusCallback {

	/**
	 * Called when the message was successfully delivered at the other Smart
	 * Connector
	 * 
	 * @param message the sent message
	 */
	void delivered(KnowledgeMessage message);

	/**
	 * Called when the message could not be delivered
	 * 
	 * @param message The message that was supposed two be sent
	 * @param t       Throwable which indicates the problem
	 */
	void deliveryFailed(KnowledgeMessage message, Throwable t);

}
