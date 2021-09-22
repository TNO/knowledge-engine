package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class MultiSensorKnowledgeInteraction extends AnswerKnowledgeInteraction {

	private static final TriplePattern.Variable VAR_R = new TriplePattern.Variable("?r");
	private static final TriplePattern.Variable VAR_S = new TriplePattern.Variable("?s");
	private static final TriplePattern.Variable VAR_O = new TriplePattern.Variable("?o");

	private final Map<TriplePattern.Literal, TriplePattern.Literal> data = new HashMap<>();

	public MultiSensorKnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:multisensor"), new TriplePattern("?s rdf:type Sensor"), new TriplePattern("?s isInRoom ?r"),
				new TriplePattern("?s isOn ?o"));
		data.put(new TriplePattern.Literal("sensorA"), new TriplePattern.Literal("room1"));
		data.put(new TriplePattern.Literal("sensorB"), new TriplePattern.Literal("room2"));
		data.put(new TriplePattern.Literal("sensorC"), new TriplePattern.Literal("room3"));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		BindingSet response = new BindingSet();
		if (bindingSet.isEmpty()) {
			// return all data
			for (Entry<Literal, Literal> e : data.entrySet()) {
				Binding b = new Binding();
				b.put(VAR_S, e.getKey());
				b.put(VAR_R, e.getValue());
				b.put(VAR_O, new TriplePattern.Literal("true"));
				response.add(b);
			}
		} else {
			// query database, every sensor is always on
			for (Binding requestBinding : bindingSet) {
				Stream<Entry<Literal, Literal>> stream = data.entrySet().stream();
				if (requestBinding.containsKey(VAR_S)) {
					// sensor provided
					stream = stream.filter(e -> {
						Literal obj = requestBinding.get(VAR_S);
						return e.getKey().equals(obj);
					});
				}
				if (requestBinding.containsKey(VAR_R)) {
					// room provided
					stream = stream.filter(e -> e.getValue().equals(requestBinding.get(VAR_R)));
				}
				if (requestBinding.containsKey(VAR_O)) {
					stream = stream.filter(e -> new TriplePattern.Literal("true").equals(requestBinding.get(VAR_O)));
				}
				List<Entry<Literal, Literal>> filterResult = stream.collect(Collectors.toList());
				for (Entry<Literal, Literal> e : filterResult) {
					Binding b = new Binding();
					b.put(VAR_S, e.getKey());
					b.put(VAR_R, e.getValue());
					// It's always on
					b.put(VAR_O, new TriplePattern.Literal("true"));
					response.add(b);
				}
			}
		}

		System.err.println("Knowledge Interaction " +

				getId() + " formulated response " + response + " based on BindingSet " + bindingSet);
		return response;
	}

}
