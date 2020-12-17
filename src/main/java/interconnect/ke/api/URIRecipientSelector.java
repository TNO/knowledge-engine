package interconnect.ke.api;

import java.net.URI;

/**
 * This is a RecipientSelector that only allows a single recipient, specified by
 * the URI of the knowledge base.
 */
public final class URIRecipientSelector extends RecipientSelector {

	public final URI knowledgeBaseId;

	public URIRecipientSelector(URI knowledgeBase) {
		super();
		this.knowledgeBaseId = knowledgeBase;
	}

	public URI getKnowledgeBaseId() {
		return knowledgeBaseId;
	}
}
