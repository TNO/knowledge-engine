package interconnect.ke.sc;

import java.net.URI;
import java.net.URL;
import java.util.List;

import interconnect.ke.api.interaction.KnowledgeInteraction;

/**
 * An {@link OtherKnowledgeBase} represents a knowledge base in the network that
 * is NOT the knowledge base that this smart connector represents.
 */
public class OtherKnowledgeBase {
	private final URI id;
	private final String name;
	private final String description;
	private final List<KnowledgeInteraction> knowledgeInteractions;
	private final URL endpoint;

	public OtherKnowledgeBase(URI anId, String aName, String aDescription, List<KnowledgeInteraction> someKnowledgeInteractions,
			URL anEndpoint) {
		this.id = anId;
		this.name = aName;
		this.description = aDescription;
		this.knowledgeInteractions = someKnowledgeInteractions;
		this.endpoint = anEndpoint;
	}

	public URI getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<KnowledgeInteraction> getKnowledgeInteractions() {
		return knowledgeInteractions;
	}

	public URL getEndpoint() {
		return endpoint;
	}
}
