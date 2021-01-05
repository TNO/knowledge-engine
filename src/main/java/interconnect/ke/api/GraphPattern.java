package interconnect.ke.api;

import java.util.List;

/**
 * A {@link GraphPattern} expresses a 'shape' of knowledge. We use Basic Graph
 * Pattern syntax from SPARQL 1.1
 * {@linkplain https://www.w3.org/TR/sparql11-query/} to represent this
 * knowledge.
 * 
 * It is a conjunction of triple patterns, where each triple pattern consists of
 * a {@code subject}, {@code predicate} and {@code object}. Each of these can be
 * either a variable or a SPARQL literal/iri.
 */
public class GraphPattern {

	/**
	 * According to Basic Graph Pattern syntax in SPARQL 1.1
	 * {@linkplain https://www.w3.org/TR/sparql11-query/}
	 */
	private final String pattern;

	/**
	 * Create a {@link GraphPattern}.
	 * 
	 * @param aPattern A string that contains a SPARQL 1.1 Basic Graph Pattern
	 *                 {@linkplain https://www.w3.org/TR/sparql11-query/}. It
	 *                 encodes a particular type of knowledge about which the
	 *                 {@link KnowledgeBase} wants to communicate with other
	 *                 {@link KnowledgeBase}.
	 */
	public GraphPattern(String aPattern) {
		this.pattern = aPattern;
	}

	/**
	 * @return A string that contains a SPARQL 1.1 Basic Graph Pattern
	 *         {@linkplain https://www.w3.org/TR/sparql11-query/}.
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * @return A list of all the variable names (excluding the '?') occurring in this
	 *         {@link GraphPattern}.
	 */
	public List<String> getVariables() {
		throw new RuntimeException("Unimplemented");
	}
}
