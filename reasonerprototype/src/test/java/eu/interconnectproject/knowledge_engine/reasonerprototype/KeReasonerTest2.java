package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;

public class KeReasonerTest2 {

	private KeReasoner reasoner;

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasoner();
		reasoner.addKnowledgeInteraction(new TransitivityTestKnowledgeInteraction());

		reasoner.addLocalRule(new LocalRule(Arrays.asList(new Triple("?a someProp ?b"), new Triple("?b someProp ?c")),
				Arrays.asList(new Triple("?a someProp ?c"))));
	}

	@Test
	public void testTransitivity() {
		// Formulate objective
		Binding b = new Binding();
		List<Triple> objective = new ArrayList<>();
		objective.add(new Triple("?thing someProp ?otherThing"));

		// Start reasoning
		BindingSet bindingSet = reasoner.reason(objective, b);

		System.out.println("Reasoning resulted in bindingset " + bindingSet);
	}

}
