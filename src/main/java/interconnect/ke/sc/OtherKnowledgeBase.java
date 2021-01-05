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
	private URI uri;
	private String name;
	private String description;
	private List<KnowledgeInteraction> knowledgeInteractions;
	private URL endpoint;

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<KnowledgeInteraction> getKnowledgeInteractions() {
		return knowledgeInteractions;
	}
	
	public void setKnowledgeInteractions(List<KnowledgeInteraction> knowledgeInteractions) {
		this.knowledgeInteractions = knowledgeInteractions;
	}

	public URL getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(URL endpoint) {
		this.endpoint = endpoint;
	}
}
