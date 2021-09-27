package eu.knowledge.engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;

import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class LocationKnowledgeInteraction extends AnswerKnowledgeInteraction {

	public LocationKnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:location"), new TriplePattern("?sensor rdf:type Sensor"), new TriplePattern("?sensor isInRoom ?room"));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		BindingSet response = new BindingSet();
		for (Binding binding : bindingSet) {
			if (binding.containsKey(new TriplePattern.Variable("?sensor"))
					&& !binding.get(new TriplePattern.Variable("?sensor")).equals(new TriplePattern.Literal("sensor1"))) {
				// no binding
			} else if (binding.containsKey(new TriplePattern.Variable("?room"))
					&& !binding.get(new TriplePattern.Variable("?room")).equals(new TriplePattern.Literal("room1"))) {
				// no binding
			} else {
				Binding b = new Binding();
				b.put(new TriplePattern.Variable("?sensor"), new TriplePattern.Literal("sensor1"));
				b.put(new TriplePattern.Variable("?room"), new TriplePattern.Literal("room1"));
				response.add(b);
			}
		}
		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
