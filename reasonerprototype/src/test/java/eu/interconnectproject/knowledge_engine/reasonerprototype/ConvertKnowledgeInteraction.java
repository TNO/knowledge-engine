package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Literal;
import eu.interconnectproject.knowledge_engine.reasonerprototype.ki.ReactKnowledgeInteraction;

public class ConvertKnowledgeInteraction extends ReactKnowledgeInteraction {

	public ConvertKnowledgeInteraction() throws URISyntaxException {
		super(new URI("urn:covert"),
				Collections.singletonList(new TriplePattern("?sensor hasTempFahrenheit ?temp_fahrenheit")),
				Collections.singletonList(new TriplePattern("?sensor hasTempCelsius ?temp_celsius")));
	}

	@Override
	public BindingSet processRequest(BindingSet bindingSet) {
		List<Binding> result = new ArrayList<>();
		for (Binding binding : bindingSet) {
			Literal sensor = binding.get(new TriplePattern.Variable("?sensor"));
			Double fahrenheit = Double.parseDouble(binding.get(new TriplePattern.Variable("?temp_fahrenheit")).getValue());
			Double celsius = ((fahrenheit - 32) * 5d) / 9d;
			Binding resultBinding = new Binding();
			resultBinding.put(new TriplePattern.Variable("?sensor"), sensor);
			resultBinding.put(new TriplePattern.Variable("?temp_celsius"), new TriplePattern.Literal(Double.toString(celsius)));
			result.add(resultBinding);
		}
		BindingSet response = new BindingSet(result);
		System.err.println("Knowledge Interaction " + getId() + " formulated response " + response
				+ " based on BindingSet " + bindingSet);
		return response;
	}

}
