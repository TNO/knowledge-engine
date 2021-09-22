package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class TransitivityTestKnowledgeInteraction extends AnswerKnowledgeInteraction {

	public TransitivityTestKnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:someObjects1"), new TriplePattern("?obj1 someProp ?obj2"));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		Binding b1 = new Binding();
		b1.put(new TriplePattern.Variable("?obj1"), new TriplePattern.Literal("a"));
		b1.put(new TriplePattern.Variable("?obj2"), new TriplePattern.Literal("b"));
		
		Binding b2 = new Binding();
		b2.put(new TriplePattern.Variable("?obj1"), new TriplePattern.Literal("b"));
		b2.put(new TriplePattern.Variable("?obj2"), new TriplePattern.Literal("c"));

		BindingSet response = new BindingSet(b1, b2);
		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
