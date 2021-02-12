package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * An {@link OtherKnowledgeBase} represents a knowledge base in the network that
 * is NOT the knowledge base that this smart connector represents.
 */
public class OtherKnowledgeBase {
	private final URI id;
	private final String name;
	private final String description;
	private final List<KnowledgeInteractionInfo> knowledgeInteractions;
	private final URL endpoint;

	public OtherKnowledgeBase(URI anId, String aName, String aDescription,
			List<KnowledgeInteractionInfo> someKnowledgeInteractions, URL anEndpoint) {
		this.id = anId;
		this.name = aName;
		this.description = aDescription;
		this.knowledgeInteractions = someKnowledgeInteractions;
		this.endpoint = anEndpoint;
	}

	public URI getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public List<KnowledgeInteractionInfo> getKnowledgeInteractions() {
		return this.knowledgeInteractions;
	}

	public URL getEndpoint() {
		return this.endpoint;
	}
}
