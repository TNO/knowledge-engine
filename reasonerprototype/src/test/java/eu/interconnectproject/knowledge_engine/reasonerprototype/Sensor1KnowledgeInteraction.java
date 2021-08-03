package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class Sensor1KnowledgeInteraction extends AnswerKnowledgeInteraction {

	public Sensor1KnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:sensor1"), new Triple("sensor1 rdf:type Sensor"),
				new Triple("sensor1 hasTempFahrenheit ?tempF"));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		Binding b = new Binding();
		b.put(new Triple.Variable("?tempF"), new Triple.Literal("80"));
		BindingSet response = new BindingSet(b);
		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
