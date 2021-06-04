package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.sse.SSEParseException;

public class BindingValidator {

	/**
	 * Throws an {@link InvalidArgumentException} if one of the bindings uses a
	 * key that is not one of the graph pattern's variables.
	 *
	 * 
	 * @param pattern
	 * @param bindings
	 */
	public void validatePartialBindings(GraphPattern pattern, BindingSet bindings) {
		if (pattern == null) {
			if (bindings == null) {
				throw new IllegalArgumentException("Bindings must be non-null.");
			} else if (bindings.size() > 0) {
				throw new IllegalArgumentException("Bindings must be empty when the graph pattern is missing.");
			}
			return;
		}
		Set<String> variables = pattern.getVariables();
		bindings.forEach(b -> {
			b.forEach((k, v) -> {
				this.validateValidBindingValue(v);
				if (!variables.contains(k)) {
					throw new IllegalArgumentException(String.format("Given binding set uses key '%s', but this does not occur as a variable in graph pattern '%s'.", k, pattern.getPattern()));
				}
			});
		});
	}
	
	public void validateCompleteBindings(GraphPattern pattern, BindingSet bindings) {
		if (pattern == null) {
			if (bindings == null) {
				throw new IllegalArgumentException("Bindings must be non-null.");
			} else if (bindings.size() > 0) {
				throw new IllegalArgumentException("Bindings must be empty when the graph pattern is missing.");
			}
			return;
		}
		Set<String> variables = pattern.getVariables();
		bindings.forEach(b -> {
			var notSeenYet = new HashSet<String>(variables);
			b.forEach((k, v) -> {
				this.validateValidBindingValue(v);
				if (!variables.contains(k)) {
					throw new IllegalArgumentException(String.format("Given binding set uses key '%s', but this does not occur as a variable in graph pattern '%s'.", k, pattern.getPattern()));
				}
				notSeenYet.remove(k);
			});
			if (!notSeenYet.isEmpty()) {
				throw new IllegalArgumentException(String.format("Expected a complete binding, but was missing some variable(s) that ARE in the graph "
					+ "pattern ('%s'), but are missing from the binding, namely: %s. Note that the question mark should NOT be included in your binding keys.", pattern.getPattern(), notSeenYet.toString()));
			}
		});
	}

	/**
	 * Throws an {@link InvalidArgumentException} if the given value is not either:
	 * - an unprefixed IRI, or
	 * - a valid literal.
	 * @param value
	 */
	public void validateValidBindingValue(String value) {
		try {
			var node = SSE.parseNode(value, new PrefixMappingMem());
			if (!(node.isLiteral() || node.isURI())) {
				throw new IllegalArgumentException(String.format("'%s' is not an unprefixed URI or literal.", value));
			}
		} catch (SSEParseException spe) {
			throw new IllegalArgumentException(String.format("'%s' is not an unprefixed URI or literal.", value));
		}
	}
}
