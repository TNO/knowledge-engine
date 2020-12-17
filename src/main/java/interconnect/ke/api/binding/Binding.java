package interconnect.ke.api.binding;

/**
 * A variable name and a corresponding value. 
 *
 */
public class Binding {

	/**
	 * The variable name (without the SPARQL question mark in front)
	 */
	private final String variableName;

	/**
	 * A RDF representation of the node, i.e. "12"^^xsd:integer
	 */
	private final String value;

	public Binding(String aVariableName, String aValue) {
		this.variableName = aVariableName;
		this.value = aValue;
	}

	/**
	 * @return The variable name associated with this binding
	 */
	public String getVariableName() {
		return this.variableName;
	}

	/**
	 * @return The valua associated with the variable name in this binding.
	 */
	public String getValue() {
		return this.value;
	}

}
