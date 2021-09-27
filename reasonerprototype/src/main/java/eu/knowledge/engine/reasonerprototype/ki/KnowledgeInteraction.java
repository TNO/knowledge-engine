package eu.knowledge.engine.reasonerprototype.ki;

import java.net.URI;

import eu.knowledge.engine.reasonerprototype.api.BindingSet;

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
