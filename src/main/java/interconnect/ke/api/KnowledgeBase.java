package interconnect.ke.api;

import java.net.URI;

public interface KnowledgeBase {

	public URI getKnowledgeBaseId() throws KnowledgeEngineException;

	public String getKnowledgeBaseDescription();

	public void smartConnectorReady();

	public void smartConnectorConnectionLost();

	public void smartConnectorConnectionRestored();

}