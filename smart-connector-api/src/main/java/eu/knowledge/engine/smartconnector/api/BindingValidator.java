package eu.knowledge.engine.smartconnector.api;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.sse.SSEParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingValidator {

	private static final Logger LOG = LoggerFactory.getLogger(BindingValidator.class);

	private static Pattern UNPREFIXED_URI_PATTERN = Pattern.compile("^<[^>]+>$");

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
		LOG.debug("validating {}", value);
		try {
			var node = SSE.parseNode(value, new PrefixMappingMem());
			if (!(node.isLiteral() || node.isURI())) {
				LOG.debug("{} is not valid because Jena said it is not a literal or URI", value);
				throw new IllegalArgumentException(String.format("'%s' is not an unprefixed URI or literal.", value));
			} else if (node.isURI()) {
				var matcher = UNPREFIXED_URI_PATTERN.matcher(value.strip());
				if (!matcher.matches()) {
					LOG.debug("{} is not valid because matcher said no (even though Jena accepted it as an URI)", value);
					throw new IllegalArgumentException(String.format("'%s' is not a valid unprefixed URI.", value));
				}
			}
		} catch (SSEParseException spe) {
			LOG.debug("{} is not valid because Jena could not parse it", value);
			throw new IllegalArgumentException(String.format("'%s' is not an unprefixed URI or literal.", value));
		}
		LOG.debug("{} is valid", value);
	}

	public void validateIncomingOutgoingAnswer(GraphPattern pattern, BindingSet incoming, BindingSet outgoing) {
		// make sure each of the outgoing bindings 'fits' on at least 1 incoming binding.
		
		// TODO: Is an empty binding set, [], simply 'syntactic sugar' for a binding
		// set with the empty binding, [{}]? Then this would be consistent.
		if (incoming.size() == 0) {
			// if there were no incoming bindings, all is good
			return;
		}

		outgoing.forEach(outgoingBinding -> {
			// Check if there is an incoming binding that is matches (is a 'sub-binding' of) the outgoing binding.
			if (incoming.stream().allMatch(incomingBinding -> !incomingBinding.isSubBindingOf(outgoingBinding))) {
				// If not, it is invalid.
				throw new IllegalArgumentException("No matching incoming binding found for outgoing binding " + outgoingBinding);
			}
		});
	}

	public void validateIncomingOutgoingReact(GraphPattern argumentPattern, GraphPattern resultPattern, BindingSet incoming, BindingSet outgoing) {
		// this one is different, because only the variables that occur in both the
		// argument and result patterns have to 'fit'.

		if (resultPattern == null && outgoing.size() > 0) {
			throw new IllegalArgumentException("Cannot have outgoing bindings when result pattern is null.");
		} else if (resultPattern != null) {
			var overlappingVariables = argumentPattern.getVariables();
			overlappingVariables.retainAll(resultPattern.getVariables());

			outgoing.forEach(outgoingBinding -> {
				// Note that here, we first transform the incoming bindings to bindings
				// with ONLY the overlapping variables, and then check if any of them
				// match with the outgoing binding. This is because the argument
				// (incoming) graph pattern can have variables that do not occur in the
				// result (outgoing) graph pattern.
				if (incoming.stream().map(incomingBinding -> incomingBinding.keepOnly(overlappingVariables)).allMatch(b -> !b.isSubBindingOf(outgoingBinding))) {
					throw new IllegalArgumentException("No matching incoming binding found for outgoing binding " + outgoingBinding);
				}
			});
		}
	}
}
