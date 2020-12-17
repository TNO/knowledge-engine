package interconnect.ke.api;

import java.util.List;

public class GraphPattern {
	/**
	 * According to Basic Graph Pattern syntax in SPARQL 1.1
	 * {@linkplain https://www.w3.org/TR/sparql11-query/}
	 */
	private final String pattern;

	public GraphPattern(String aPattern) {
		this.pattern = aPattern;
	}

	public String getPattern() {
		return this.pattern;
	}

	/**
	 * @return A list of all the variables (including the '?') occuring in this
	 * graph pattern.
	 */
	public List<String> getVariables() {
		throw new RuntimeException("Unimplemented");
	}
}