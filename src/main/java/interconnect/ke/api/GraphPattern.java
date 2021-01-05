package interconnect.ke.api;

import java.io.StringReader;
import java.util.List;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.lang.arq.ARQParser;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOG = LoggerFactory.getLogger(GraphPattern.class);

	private PrefixMapping prefixes;

	/**
	 * According to Basic Graph Pattern syntax in SPARQL 1.1
	 * {@linkplain https://www.w3.org/TR/sparql11-query/}
	 */
	private final String pattern;

	public GraphPattern(PrefixMapping aPrefixMapping, String aPattern) {
		this.prefixes = aPrefixMapping;
		this.pattern = aPattern;
	}

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
		this.prefixes = PrefixMapping.Standard;
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

	public ElementPathBlock getGraphPattern() throws ParseException {

		ElementGroup eg;
		ARQParser parser = new ARQParser(new StringReader(this.pattern));
		parser.setPrologue(new Prologue(this.prefixes));

		Element e = null;
		e = parser.GroupGraphPatternSub();
		LOG.trace("parsed knowledge: {}", e);
		eg = (ElementGroup) e;
		Element last = eg.getLast();

		if (!(last instanceof ElementPathBlock)) {
			LOG.error("This knowledge '{}' should be parseable to a ElementPathBlock", this.pattern);
			throw new ParseException(
					"The knowledge should be parseable to a ARQ ElementPathBlock (i.e. a BasicGraphPattern in the SPARQL syntax specification)");
		}
		return (ElementPathBlock) eg.getLast();

	}

}
