package eu.interconnectproject.knowledge_engine.smartconnector.api;

import java.io.StringReader;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Node_Variable;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.lang.arq.ARQParser;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.util.GraphPatternSerialization;

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

	private final ElementPathBlock epb;

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
		String patternWithPrefixes = String.join("", somePatternFragments);
		
		// First we parse the graph pattern with the given prefixes.
		ElementPathBlock epb;
		try {
			epb = this.parseGraphPattern(aPrefixMapping, patternWithPrefixes);
		} catch (ParseException e) {
			throw new IllegalArgumentException(String.format("Invalid graph pattern: %s", patternWithPrefixes), e);
		}

		// From this, we serialize it to a canonical pattern with full IRIs.
		this.pattern = GraphPatternSerialization.serialize(epb);

		// Then we parse it from the canonical pattern to an ElementPathBlock again
		// (we cache this object).
		try {
			this.epb = this.parseGraphPattern(new PrefixMappingZero(), this.pattern);
		} catch (ParseException e) {
			throw new IllegalArgumentException(String.format("Invalid graph pattern: %s", this.pattern), e);
		}
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
		this(new PrefixMappingZero(), somePatternFragments);
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
	public Set<String> getVariables() {
		var triples = this.getGraphPattern().getPattern().getList();
		return triples.stream()
			// Redirect the subjects, predicates and objects to a single stream of nodes.
			.flatMap(t -> Stream.of(t.getSubject(), t.getPredicate(), t.getObject()))
			// Map the nodes to variable names if they're variables, and otherwise `null`.
			.map(n -> {
				if (n instanceof Node_Variable) {
					return ((Node_Variable) n).getName();
				} else {
					return null;
				}
			})
			// Filter out the nulls.
			.filter(Objects::nonNull)
			// Collect them into a set.
			.collect(Collectors.toSet());
	}

	public ElementPathBlock getGraphPattern() {
		return this.epb;
	}

	private ElementPathBlock parseGraphPattern(PrefixMapping prefixes, String pattern) throws ParseException {

		LOG.trace("prefixes: {}- pattern: {}", prefixes, pattern);

		ElementGroup eg;
		ARQParser parser = new ARQParser(new StringReader(pattern));
		parser.setPrologue(new Prologue(prefixes));

		Element e = null;
		e = parser.GroupGraphPatternSub();
		LOG.trace("parsed knowledge: {}", e);
		eg = (ElementGroup) e;
		Element last = eg.getLast();

		if (!(last instanceof ElementPathBlock)) {
			LOG.error("This knowledge '{}' should be parseable to a ElementPathBlock", pattern);
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
