package eu.interconnectproject.knowledge_engine.reasonerprototype.ki;

import java.net.URI;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;

public abstract class KnowledgeInteraction {

	private final URI id;

	public KnowledgeInteraction(URI id) {
		this.id = id;
	}

	public URI getId() {
		return id;
	}

	public abstract BindingSet processRequest(BindingSet bindingSet);

}
