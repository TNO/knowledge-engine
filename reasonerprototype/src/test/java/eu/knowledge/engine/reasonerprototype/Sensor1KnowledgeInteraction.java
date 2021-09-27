package eu.knowledge.engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;

import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class Sensor1KnowledgeInteraction extends AnswerKnowledgeInteraction {

	public Sensor1KnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:sensor1"), new TriplePattern("sensor1 rdf:type Sensor"),
				new TriplePattern("sensor1 hasTempFahrenheit ?tempF"));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		Binding b = new Binding();
		b.put(new TriplePattern.Variable("?tempF"), new TriplePattern.Literal("80"));
		BindingSet response = new BindingSet(b);
		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
