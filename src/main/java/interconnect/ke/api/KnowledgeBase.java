package interconnect.ke.api;

import java.net.URI;

/**
 * The {@link KnowledgeBase} is a generic name for the
 * app/service/platform/database that wants to exchange data in an interoperable
 * manner.
 */
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
	 * This method is called by the smart connector when it is connected and ready.
	 */
	public void smartConnectorReady(SmartConnector aSC);

	/**
	 * This method is called by the smart connector when it has lost its connection
	 * to the knowledge network.
	 */
	public void smartConnectorConnectionLost(SmartConnector aSC);

	/**
	 * This method is called by the smart connector when it restores its connection
	 * after it has been lost.
	 */
	public void smartConnectorConnectionRestored(SmartConnector aSC);

	/**
	 * This method is called by the smart connector when it restores its connection
	 * after it has been lost.
	 */
	public void smartConnectorStopped(SmartConnector aSC);
}