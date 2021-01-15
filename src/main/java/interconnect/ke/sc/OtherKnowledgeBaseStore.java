package interconnect.ke.sc;

import java.util.Set;

import interconnect.ke.api.KnowledgeBase;
import interconnect.ke.api.interaction.KnowledgeInteraction;
import interconnect.ke.api.runtime.KnowledgeDirectory;

/**
 * An {@link OtherKnowledgeBaseStore} is responsible for keeping track of
 * metadata about knowledge bases in the knowledge network. In the MVP, it
 * should poll the network periodically for other {@link KnowledgeBase}s'
 * {@link KnowledgeInteraction}s and their {@link SmartConnectorImpl}s'
 * endpoints.
 *
 * It uses the {@link KnowledgeDirectory} to discover other smart connectors.
 */
public interface OtherKnowledgeBaseStore {

	/**
	 * Start the updating of the store.
	 */
	void start();

	/**
	 * Stop the updating of the store.
	 */
	void stop();

	/**
	 * @return The current list of {@link OtherKnowledgeBase}s.
	 */
	Set<OtherKnowledgeBase> getOtherKnowledgeBases();

}
