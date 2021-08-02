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
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.AnswerKnowledgeInteraction;

public class MultiSensorKnowledgeInteraction extends AnswerKnowledgeInteraction {

	private static final Triple.Variable VAR_R = new Triple.Variable("?r");
	private static final Triple.Variable VAR_S = new Triple.Variable("?s");
	private static final Triple.Variable VAR_O = new Triple.Variable("?o");

	private final Map<Triple.Literal, Triple.Literal> data = new HashMap<>();

	public MultiSensorKnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:multisensor"), new Triple("?s rdf:type Sensor"), new Triple("?s isInRoom ?r"),
				new Triple("?s isOn ?o"));
		data.put(new Triple.Literal("sensorA"), new Triple.Literal("room1"));
		data.put(new Triple.Literal("sensorB"), new Triple.Literal("room2"));
		data.put(new Triple.Literal("sensorC"), new Triple.Literal("room3"));
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
				b.put(VAR_O, new Triple.Literal("true"));
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
					stream = stream.filter(e -> new Triple.Literal("true").equals(requestBinding.get(VAR_O)));
				}
				List<Entry<Literal, Literal>> filterResult = stream.collect(Collectors.toList());
				for (Entry<Literal, Literal> e : filterResult) {
					Binding b = new Binding();
					if (!requestBinding.containsKey(VAR_S)) {
						b.put(VAR_S, e.getKey());
					}
					if (!requestBinding.containsKey(VAR_R)) {
						b.put(VAR_R, e.getValue());
					}
					if (!requestBinding.containsKey(VAR_O)) {
						// It's always on
						b.put(VAR_O, new Triple.Literal("true"));
					}
					response.add(b);
				}
			}
		}

		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
