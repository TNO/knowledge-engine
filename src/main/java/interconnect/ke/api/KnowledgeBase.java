package interconnect.ke.api;

import java.net.URI;

public interface KnowledgeBase {

	/**
	 * @return Globally unique identifier for this knowledge base.
	 */
	public URI getKnowledgeBaseId();

	/**
	 * @return Human-friendly name of this knowledge base.
	 */
	public String getKnowledgeBaseName();
	
	/**
	 * @return A short description of this knowledge base.
	 */
	public String getKnowledgeBaseDescription();

	
	/**
	 * This method is called by the smart connector when it is connected and
	 * ready.
	 */
	public void smartConnectorReady();
	
	/**
	 * This method is called by the smart connector when it has lost its
	 * connection to the knowledge network.
	 */
	public void smartConnectorConnectionLost();

	/**
	 * This method is called by the smart connector when it restores its
	 * connection after it has been lost.
	 */
	public void smartConnectorConnectionRestored();
}