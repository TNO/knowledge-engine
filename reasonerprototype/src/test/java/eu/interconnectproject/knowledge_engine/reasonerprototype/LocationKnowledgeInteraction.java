package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class LocationKnowledgeInteraction extends AnswerKnowledgeInteraction {

	public LocationKnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:location"), new Triple("?sensor rdf:type Sensor"), new Triple("?sensor isInRoom ?room"));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		BindingSet response = new BindingSet();
		for (Binding binding : bindingSet) {
			if (binding.containsKey(new Triple.Variable("?sensor"))
					&& !binding.get(new Triple.Variable("?sensor")).equals(new Triple.Literal("sensor1"))) {
				// no binding
			} else if (binding.containsKey(new Triple.Variable("?room"))
					&& !binding.get(new Triple.Variable("?room")).equals(new Triple.Literal("room1"))) {
				// no binding
			} else {
				Binding b = new Binding();
				b.put(new Triple.Variable("?sensor"), new Triple.Literal("sensor1"));
				b.put(new Triple.Variable("?room"), new Triple.Literal("room1"));
				response.add(b);
			}
		}
		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
