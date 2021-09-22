package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;

public class KeReasonerTest {

	private KeReasoner reasoner;

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasoner();
		reasoner.addKnowledgeInteraction(new MultiSensorKnowledgeInteraction());
		reasoner.addKnowledgeInteraction(new Sensor1KnowledgeInteraction());
		reasoner.addKnowledgeInteraction(new ConvertKnowledgeInteraction());
		reasoner.addKnowledgeInteraction(new LocationKnowledgeInteraction());
		reasoner.addLocalRule(new LocalRule(Collections.singletonList(new TriplePattern("?s rdf:type Sensor")),
				Collections.singletonList(new TriplePattern("?s rdf:type Thing"))));
	}

	@Test
	public void testConversion() throws URISyntaxException {
		// Formulate objective
		Binding b = new Binding();
		b.put(new TriplePattern.Variable("?sens"), new TriplePattern.Literal("sensor1"));
		List<TriplePattern> objective = Collections.singletonList(new TriplePattern("?sens hasTempCelsius ?tc"));

		// Start reasoning
		BindingSet bindingSet = reasoner.reason(objective, b);

		System.out.println("Reasoning resulted in bindingset " + bindingSet);
	}

	@Test
	public void testMultiTripleObjective() {
		// Formulate objective
		Binding b = new Binding();
		b.put(new TriplePattern.Variable("?room"), new TriplePattern.Literal("room1"));
		List<TriplePattern> objective = new ArrayList<>();
		objective.add(new TriplePattern("?sensor rdf:type Sensor"));
		objective.add(new TriplePattern("?sensor isInRoom ?room"));
//		objective.add(new Triple("?sensor isOn ?isOn"));

		// Start reasoning
		BindingSet bindingSet = reasoner.reason(objective, b);

		System.out.println("Reasoning resulted in bindingset " + bindingSet);
	}

	@Test
	public void testSubset() {
		// Formulate objective
		Binding b = new Binding();
		List<TriplePattern> objective = new ArrayList<>();
		objective.add(new TriplePattern("?sensor isInRoom ?room"));

		// Start reasoning
		BindingSet bindingSet = reasoner.reason(objective, b);

		System.out.println("Reasoning resulted in bindingset " + bindingSet);
	}

	@Test
	public void testCombiningDifferentWays() {
		// Formulate objective
		Binding b = new Binding();
		List<TriplePattern> objective = new ArrayList<>();
		objective.add(new TriplePattern("?SENSOR rdf:type Sensor"));
		objective.add(new TriplePattern("?SENSOR isInRoom ?ROOM"));

		// Start reasoning
		BindingSet bindingSet = reasoner.reason(objective, b);

		System.out.println("Reasoning resulted in bindingset " + bindingSet);
	}

	@Test
	public void testLocalRule() {
		// Formulate objective
		Binding b = new Binding();
		List<TriplePattern> objective = new ArrayList<>();
		objective.add(new TriplePattern("?thing rdf:type Thing"));

		// Start reasoning
		BindingSet bindingSet = reasoner.reason(objective, b);

		System.out.println("Reasoning resulted in bindingset " + bindingSet);
	}

}
