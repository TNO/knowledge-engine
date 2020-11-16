package interconnect.ke.api;

import java.net.URI;

public final class FixedRecipientSelector extends RecipientSelector {

	public final URI knowledgeBaseId;

	public FixedRecipientSelector(URI knowledgeBase) {
		super();
		this.knowledgeBaseId = knowledgeBase;
	}

	public URI getKnowledgeBaseId() {
		return knowledgeBaseId;
	}
}
