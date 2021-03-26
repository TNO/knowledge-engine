package eu.interconnectproject.knowledge_engine.smartconnector.api;

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

	private final PrefixMapping prefixes;

	/**
	 * According to Basic Graph Pattern syntax in SPARQL 1.1
	 * {@linkplain https://www.w3.org/TR/sparql11-query/}
	 */
	private final String pattern;

	/**
	 *
	 * @param aPrefixMapping       A prefix mapping with prefixes that are allowed
	 *                             to be used in the pattern.
	 * @param somePatternFragments Strings that, when concatenated, contain a
	 *                             SPARQL 1.1 Basic Graph Pattern
	 *                             {@linkplain https://www.w3.org/TR/sparql11-query/}.
	 *                             It encodes a particular type of knowledge about
	 *                             which the {@link KnowledgeBase} wants to
	 *                             communicate with other {@link KnowledgeBase}.
	 *                             Cannot be null.
	 * @throws ParseException
	 */
	public GraphPattern(PrefixMapping aPrefixMapping, String... somePatternFragments) throws IllegalArgumentException {
		this.prefixes = aPrefixMapping;
		this.pattern = String.join("", somePatternFragments);
		this.validate();
	}

	/**
	 * Create a {@link GraphPattern}.
	 *
	 * @param somePatternFragments Strings that, when concatenated, contain a SPARQL
	 *                             1.1 Basic Graph Pattern
	 *                             {@linkplain https://www.w3.org/TR/sparql11-query/}.
	 *                             It encodes a particular type of knowledge about
	 *                             which the {@link KnowledgeBase} wants to
	 *                             communicate with other {@link KnowledgeBase}.
	 *                             Cannot be null.
	 * @throws ParseException
	 */
	public GraphPattern(String... somePatternFragments) {
		this(PrefixMapping.Standard, somePatternFragments);
	}

	public void validate() {
		try {
			this.getGraphPattern();
		} catch (ParseException e) {
			throw new IllegalArgumentException(String.format("Invalid graph pattern: %s", this.pattern), e);
		}
	}

	/**
	 * @return A string that contains a SPARQL 1.1 Basic Graph Pattern
	 *         {@linkplain https://www.w3.org/TR/sparql11-query/}.
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * @return A list of all the variable names (excluding the '?') occurring in
	 *         this {@link GraphPattern}.
	 */
	public List<String> getVariables() {
		throw new RuntimeException("Unimplemented");
	}

	public ElementPathBlock getGraphPattern() throws ParseException {

		LOG.trace("prefixes: {}- pattern: {}", this.prefixes, this.pattern);

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

	@Override
	public String toString() {
		return "GraphPattern [" + (this.pattern != null ? "pattern=" + this.pattern : "") + "]";
	}

}
