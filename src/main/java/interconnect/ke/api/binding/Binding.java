package interconnect.ke.api.binding;

import java.util.Map;

import java.util.HashMap;

/**
 * The bindings of a set of variables.
 * 
 * 
 * Note that not all variables in the graph pattern have to be bound. Even
 * though it is logically a java.util.Map, we wrap it because we want to
 * validate the values.
 */
public class Binding {
	// TODO: Implement/extend (Hash)Map<String, String> for easier iteration?

	private final Map<String, String> map = new HashMap<>();

	public Binding() {
	}

	/**
	 * Add a variable/value pair to this binding.
	 * 
	 * @param aVariableName The variable name to add (note that variable names should
	 *                     not start with a question mark).
	 * @param aValue        The value of the variable. Note that these should be
	 *                     SPARQL Literals
	 *                     ({@link https://www.w3.org/TR/sparql11-query/#QSynLiterals})
	 *                     or URIs
	 *                     ({@link https://www.w3.org/TR/sparql11-query/#QSynIRI}).
	 * @throws IllegalArgumentException when the variable or value is incorrect.
	 */
	public void put(String aVariableName, String aValue) throws IllegalArgumentException {
		this.validateEntry(aVariableName, aValue);
		this.map.put(aVariableName, aValue);
	}

	/**
	 * Retrieve the value of a variable in this binding.
	 * 
	 * @param aVariableName The name of the variable for which to retrieve the value
	 *                     (note that variable names should not start with a
	 *                     question mark).
	 * @return The value of {@code variableName} or {@code null} if the variable
	 *         does not exist. Note that these a SPARQL Literals
	 *         ({@link https://www.w3.org/TR/sparql11-query/#QSynLiterals}) or URIs
	 *         ({@link https://www.w3.org/TR/sparql11-query/#QSynIRI}).
	 */
	public String get(String aVariableName) {
		return this.map.get(aVariableName);
	}

	private void validateEntry(String aVariableName, String aValue) throws IllegalArgumentException {
		if (aVariableName == null || aVariableName.startsWith("?"))
			throw new IllegalArgumentException("Variable names should not be null or start with a question mark.");

		if (aValue == null /*|| not a SPARQL literal/iri*/ )
			throw new IllegalArgumentException("Variable values should not be null and follow SPARQL literal/iri syntax.");
	}
}
