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

	public GraphPattern(String aPattern) {
		this.prefixes = PrefixMapping.Standard;
		this.pattern = aPattern;
	}

	public String getPattern() {
		return this.pattern;
	}

	/**
	 * @return A list of all the variables (excluding the '?') occuring in this
	 *         graph pattern.
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
