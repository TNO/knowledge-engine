package eu.knowledge.engine.smartconnector.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * The bindings of a set of variables.
 *
 *
 * Note that not all variables in the graph pattern have to be bound. Even
 * though it is logically a java.util.Map, we wrap it because we want to
 * validate the values.
 */
public class Binding {

	private final Map<String, String> map = new HashMap<>();

	public Binding() {
	}

	/**
	 * Construct a Binding from a QuerySolution
	 * 
	 * @param qs
	 */
	public Binding(QuerySolution qs) {
		Iterator<String> vars = qs.varNames();
		while (vars.hasNext()) {
			String var = vars.next();
			this.put(var, FmtUtils.stringForNode(qs.get(var).asNode(), (PrefixMapping) null));
		}
	}

	/**
	 * Add a variable/value pair to this binding.
	 *
	 * @param aVariableName The variable name to add (note that variable names
	 *                      should not start with a question mark).
	 * @param aValue        The value of the variable. Note that these should be
	 *                      SPARQL Literals
	 *                      ({@link https://www.w3.org/TR/sparql11-query/#QSynLiterals})
	 *                      or URIs
	 *                      ({@link https://www.w3.org/TR/sparql11-query/#QSynIRI}).
	 * @throws IllegalArgumentException when the variable or value is incorrect.
	 */
	public void put(String aVariableName, String aValue) throws IllegalArgumentException {
		this.validateEntry(aVariableName, aValue);
		this.map.put(aVariableName, aValue);
	}

	public boolean containsKey(String aVariableName) {
		return this.map.containsKey(aVariableName);
	}

	/**
	 * Retrieve the value of a variable in this binding.
	 *
	 * @param aVariableName The name of the variable for which to retrieve the value
	 *                      (note that variable names should not start with a
	 *                      question mark).
	 * @return The value of {@code variableName} or {@code null} if the variable
	 *         does not exist. Note that these a SPARQL Literals
	 *         ({@link https://www.w3.org/TR/sparql11-query/#QSynLiterals}) or URIs
	 *         ({@link https://www.w3.org/TR/sparql11-query/#QSynIRI}).
	 */
	public String get(String aVariableName) {
		return this.map.get(aVariableName);
	}

	public Binding keepOnly(Set<String> variables) {
		var b = new Binding();
		for (var a : variables) {
			b.put(a, this.get(a));
		}
		return b;
	}

	public Set<String> getVariables() {
		return this.map.keySet();
	}

	private void validateEntry(String aVariableName, String aValue) throws IllegalArgumentException {
		if (aVariableName == null || aVariableName.startsWith("?")) {
			throw new IllegalArgumentException("Variable names should not be null or start with a question mark.");
		}

		if (aValue == null /* || not a SPARQL literal/iri */ ) {
			throw new IllegalArgumentException(
					"Variable values should not be null and follow SPARQL literal/iri syntax.");
		}
	}

	@Override
	public Binding clone() {
		Binding b = new Binding();
		for (var a : this.map.entrySet()) {
			b.put(a.getKey(), a.getValue());
		}
		return b;
	}

	public int size() {
		return this.map.size();
	}

	@Override
	public String toString() {
		return "Binding [" + (this.map != null ? "map=" + this.map : "") + "]";
	}

	/**
	 * See {@link java.util.Map#forEach}
	 * 
	 * @param action
	 */
	public void forEach(BiConsumer<String, String> action) {
		this.map.forEach(action);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Binding)) {
			return false;
		}
		Binding other = (Binding) obj;
		if (map == null) {
			if (other.map != null) {
				return false;
			}
		} else if (!map.equals(other.map)) {
			return false;
		}
		return true;
	}

}
