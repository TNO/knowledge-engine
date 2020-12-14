package interconnect.ke.api;

import java.net.URI;

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
