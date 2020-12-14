package interconnect.ke.api;

public class GraphPattern {

	/**
	 * According to Basic Graph Pattern syntax in SPARQL 1.1 {@linkplain https://www.w3.org/TR/sparql11-query/}
	 */
	private String pattern;

	public GraphPattern(String aPattern) {
		this.pattern = aPattern;
	}

	public String getPattern() {
		return this.pattern;
	}
}