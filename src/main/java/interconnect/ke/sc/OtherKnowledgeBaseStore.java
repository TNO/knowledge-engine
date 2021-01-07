package interconnect.ke.sc;

import java.util.List;

import interconnect.ke.api.PostResult;
import interconnect.ke.api.interaction.KnowledgeInteraction;

/**
 * An {@link OtherKnowledgeBaseStore} is responsible for keeping track of
 * metadata about knowledge bases in the knowledge network. In the MVP, it
 * should poll the network periodically for other {@link KnowledgeBase}s'
 * {@link KnowledgeInteraction}s and their {@link SmartConnectorImpl}s' endpoints.
 * 
 * It uses the {@link KnowledgeDirectory} to discover other smart connectors.
 */
public interface OtherKnowledgeBaseStore {
	/**
	 * @return The current list of {@link OtherKnowledgeBase}s.
	 */
	public List<OtherKnowledgeBase> getOtherKnowledgeBases();

	/**
	 * Synchronize the store with metadata about other knowledge bases.
	 *
	 * TODO: Do we really want to leave it up to the caller of this method to
	 * provide a valid PostResult or do we want to constrain this some more?
	 * 
	 * TODO: (After MVP) What about knowledge bases that stopped?
	 *
	 * @param aPostResult A {@link PostResult} that contains bindings that have
	 * metadata about other knowledge bases. It MUST contain bindings for an
	 * agreed set of variables that describe other knowledge bases.
	 */
	// public void synchronizeOtherKnowledeBaseMetadata(PostResult aPostResult);
}
