package eu.interconnectproject.knowledge_engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.graph.isomorphism.TypedVF2IsomorphismTester;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.GraphPatternMatcher;
import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

public class TestGraphPatternMatch {

	private static final Logger LOG = LoggerFactory.getLogger(TestGraphPatternMatch.class);

	@Test
	public void test() throws ParseException {

		GraphPattern gp1 = new GraphPattern("?a ?p1 ?b. ?a <p1> ?b.");
		GraphPattern gp2 = new GraphPattern("?c ?p2 ?d. ?c <p1> ?d.");

		TypedVF2IsomorphismTester tester = new TypedVF2IsomorphismTester();
		LOG.info("Are isomorph? {}", GraphPatternMatcher.areIsomorphic(gp1, gp2));
		Map<Integer, Integer> isomorphism = GraphPatternMatcher.getIsomorphisms(gp1, gp2);
		LOG.info("iso: {}", isomorphism);

	}

	@Test
	public void testElementEquality() throws ParseException {

		String[][] positivePairs = new String[][] {

				// @formatter: off

				new String[] { "?a ?b ?c", "?x ?y ?z" },
				new String[] { "?id <type> <HVT> . ?id <hasName> ?name . ?obs <type> <Observation> .",
						"?a <type> <Observation> . ?c <type> <HVT> . ?c <hasName> ?d ." },
				new String[] {
						"?id <type> <HVT> .\n" + "?id <hasName> ?name .\n" + "?obs <type> <Observation> .\n"
								+ "?obs <hasFeatureOfInterest> ?id .\n" + "?obs <observedProperty> <Position> .\n"
								+ "?obs <hasSimpleResult> ?position .",

						"?a <hasSimpleResult> ?b .\n" + "?a <observedProperty> <Position> .\n"
								+ "?a <hasFeatureOfInterest> ?c .\n" + "?a <type> <Observation> .\n"
								+ "?c <hasName> ?d .\n" + "?c <type> <HVT> ."

				},
				new String[] { "?a <test> ?b . ?b <test> ?c . ?c <test> ?a .",
						"?z <test> ?x . ?y <test> ?z . ?x <test> ?y ." },
				new String[] {
						"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontology.tno.nl/defense/v1905/hvt/High_Value_Target> .\n"
								+ "?id <http://ontology.tno.nl/defense/v1905/hvt/hasName> ?name .\n"
								+ "?obs <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/sosa/Observation> .\n"
								+ "?obs <http://www.w3.org/ns/sosa/hasFeatureOfInterest> ?id .\n"
								+ "?obs <http://www.w3.org/ns/sosa/observedProperty> <http://ontology.tno.nl/defense/v1905/hvt/Position> .\n"
								+ "?obs <http://www.w3.org/ns/sosa/hasSimpleResult> ?position .",

						"?a <http://www.w3.org/ns/sosa/hasSimpleResult> ?b .\n"
								+ "?a <http://www.w3.org/ns/sosa/observedProperty> <http://ontology.tno.nl/defense/v1905/hvt/Position> .\n"
								+ "?a <http://www.w3.org/ns/sosa/hasFeatureOfInterest> ?c .\n"
								+ "?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/sosa/Observation> .\n"
								+ "?c <http://ontology.tno.nl/defense/v1905/hvt/hasName> ?d .\n"
								+ "?c <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontology.tno.nl/defense/v1905/hvt/High_Value_Target> ."

				},

				new String[] {
						"?kb1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#KnowledgeBase> . ?kb1 <https://www.tno.nl/energy/ontology/interconnect#hasName> ?name1 . ?kb1 <https://www.tno.nl/energy/ontology/interconnect#hasDescription> ?description . ?kb1 <https://www.tno.nl/energy/ontology/interconnect#hasKnowledgeInteraction> ?ki1 . ?ki1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?kiType1 . ?ki1 <https://www.tno.nl/energy/ontology/interconnect#isMeta> ?isMeta1 . ?ki1 <https://www.tno.nl/energy/ontology/interconnect#hasCommunicativeAct> ?act1 . ?act1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#CommunicativeAct> . ?act1 <https://www.tno.nl/energy/ontology/interconnect#hasRequirement> ?req1 . ?act1 <https://www.tno.nl/energy/ontology/interconnect#hasSatisfaction> ?sat1 . ?req1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?reqType1 . ?sat1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?satType1 . ?ki1 <https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern> ?gp1 . ?ki1 ?patternType1 ?gp1 . ?gp1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#GraphPattern> . ?gp1 <https://www.tno.nl/energy/ontology/interconnect#hasPattern> ?pattern1 . ",
						"?kb2 <https://www.tno.nl/energy/ontology/interconnect#hasName> ?name2 . ?kb2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#KnowledgeBase> . ?kb2 <https://www.tno.nl/energy/ontology/interconnect#hasDescription> ?description . ?kb2 <https://www.tno.nl/energy/ontology/interconnect#hasKnowledgeInteraction> ?ki2 . ?ki2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?kiType2 . ?ki2 <https://www.tno.nl/energy/ontology/interconnect#isMeta> ?isMeta2 . ?ki2 <https://www.tno.nl/energy/ontology/interconnect#hasCommunicativeAct> ?act2 . ?act2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#CommunicativeAct> . ?act2 <https://www.tno.nl/energy/ontology/interconnect#hasRequirement> ?req2 . ?act2 <https://www.tno.nl/energy/ontology/interconnect#hasSatisfaction> ?sat2 . ?req2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?reqType2 . ?sat2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?satType2 . ?ki2 <https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern> ?gp2 . ?ki2 ?patternType2 ?gp2 . ?gp2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#GraphPattern> . ?gp2 <https://www.tno.nl/energy/ontology/interconnect#hasPattern> ?pattern2 . " }

				// @formatter: on
		};

		for (String[] pair : positivePairs) {
			GraphPattern ks1 = new GraphPattern(pair[0]);
			GraphPattern ks2 = new GraphPattern(pair[1]);

			boolean doesMatch = GraphPatternMatcher.areIsomorphic(ks1, ks2);

			LOG.info("SSpace graph should be equal: {}", doesMatch);
			assertTrue(doesMatch);

			LOG.info("--------------------");
		}

		String[][] negativePairs = new String[][] {

				// @formatter: off

				new String[] { "?a <http://www.tno.nl/test2> ?b", "?a <http://www.tno.nl/test1> ?b" },
				new String[] { "?a <http://www.tno.nl/test1> ?b . ?b <http://www.tno.nl/test2> ?a .",
						"?a <http://www.tno.nl/test1> ?b . ?a <http://www.tno.nl/test2> ?b ." },
				new String[] { "?a <http://www.tno.nl/test> ?b . ?b <http://www.tno.nl/test>?a .",
						"?b <http://www.tno.nl/test> ?a . ?b <http://www.tno.nl/test> ?a ." },
				new String[] { "?a <http://www.tno.nl/test1> ?b . ?b <http://www.tno.nl/test2> ?a .",
						"?b <http://www.tno.nl/test1> ?a . ?b <http://www.tno.nl/test2> ?a ." },

				new String[] { "  ?b1 <http://test.tno.nl/hond> ?d . ?a1 ?aap1 ?b1 . ?a ?aap1 ?b .",
						"?a2 ?aap2 ?b . ?b <http://test.tno.nl/hond2> ?d . ?a ?aap2 ?b" },
				new String[] {
						"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontology.tno.nl/defense/v1905/hvt/High_Value_Target> .\n"
								+ "?id <http://ontology.tno.nl/defense/v1905/hvt/hasName> ?name .\n"
								+ "?obs <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/sosa/Observation> .\n"
								+ "?obs <http://www.w3.org/ns/sosa/hasFeatureOfInterest> ?id .\n"
								+ "?obs <http://www.w3.org/ns/sosa/observedProperty> <http://ontology.tno.nl/defense/v1905/hvt/Position> .\n"
								+ "?obs <http://www.w3.org/ns/sosa/hasSimpleResult> ?position .",

						"?a <http://www.w3.org/ns/sosa/hasSimpleResult> ?b .\n"
								+ "?a <http://www.w3.org/ns/sosa/observedProperty> <http://ontology.tno.nl/defense/v1905/hvt/Position> .\n"
								+ "?a <http://www.w3.org/ns/sosa/hasFeatureOfInterest> ?c .\n"
								+ "?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/sosa/Observation> .\n"
								+ "?c <http://ontology.tno.nl/defense/v1905/hvt/hasName2> ?d .\n"
								+ "?c <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontology.tno.nl/defense/v1905/hvt/High_Value_Target> ."

				}

				// @formatter: on
		};

		for (String[] pair : negativePairs) {
			GraphPattern ks1 = new GraphPattern(pair[0]);
			GraphPattern ks2 = new GraphPattern(pair[1]);

			boolean doesMatch = GraphPatternMatcher.areIsomorphic(ks1, ks2);

			LOG.info("SSpace graph shoud not be equal: {}", doesMatch);
			assertFalse(doesMatch);

			LOG.info("--------------------");
		}
	}

	@Test
	public void test2() {
		var prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		GraphPattern gp = new GraphPattern(prefixes, "?kb rdf:type kb:KnowledgeBase .", "?kb kb:hasName ?name .",
				"?kb kb:hasDescription ?description .", "?kb kb:hasKnowledgeInteraction ?ki .",
				"?ki rdf:type ?kiType .", "?ki kb:isMeta ?isMeta .", "?ki kb:hasCommunicativeAct ?act .",
				"?act rdf:type kb:CommunicativeAct .", "?act kb:hasRequirement ?req .",
				"?act kb:hasSatisfaction ?sat .", "?req rdf:type ?reqType .", "?sat rdf:type ?satType .",
				"?ki kb:hasGraphPattern ?gp .", "?ki ?patternType ?gp .", "?gp rdf:type kb:GraphPattern .",
				"?gp kb:hasPattern ?pattern .");

		System.out.println(convertToPattern(gp));

		assertTrue(true);
	}

	@Test
	public void test3() {
		String[] patterns = new String[] {
				"?kb1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#KnowledgeBase> . ?kb1 <https://www.tno.nl/energy/ontology/interconnect#hasName> ?name1 . ?kb1 <https://www.tno.nl/energy/ontology/interconnect#hasDescription> ?description1 . ?kb1 <https://www.tno.nl/energy/ontology/interconnect#hasKnowledgeInteraction> ?ki1 . ?ki1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?kiType1 . ?ki1 <https://www.tno.nl/energy/ontology/interconnect#isMeta> ?isMeta1 . ?ki1 <https://www.tno.nl/energy/ontology/interconnect#hasCommunicativeAct> ?act1 . ?act1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#CommunicativeAct> . ?act1 <https://www.tno.nl/energy/ontology/interconnect#hasRequirement> ?req1 . ?act1 <https://www.tno.nl/energy/ontology/interconnect#hasSatisfaction> ?sat1 . ?req1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?reqType1 . ?sat1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?satType1 . ?ki1 <https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern> ?gp1 . ?ki1 ?patternType1 ?gp1 . ?gp1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#GraphPattern> . ?gp1 <https://www.tno.nl/energy/ontology/interconnect#hasPattern> ?pattern1 . ",
				"?kb2 <https://www.tno.nl/energy/ontology/interconnect#hasName> ?name2 . ?kb2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#KnowledgeBase> . ?kb2 <https://www.tno.nl/energy/ontology/interconnect#hasDescription> ?description2 . ?kb2 <https://www.tno.nl/energy/ontology/interconnect#hasKnowledgeInteraction> ?ki2 . ?ki2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?kiType2 . ?ki2 <https://www.tno.nl/energy/ontology/interconnect#isMeta> ?isMeta2 . ?ki2 <https://www.tno.nl/energy/ontology/interconnect#hasCommunicativeAct> ?act2 . ?act2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#CommunicativeAct> . ?act2 <https://www.tno.nl/energy/ontology/interconnect#hasRequirement> ?req2 . ?act2 <https://www.tno.nl/energy/ontology/interconnect#hasSatisfaction> ?sat2 . ?req2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?reqType2 . ?sat2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?satType2 . ?ki2 <https://www.tno.nl/energy/ontology/interconnect#hasGraphPattern> ?gp2 . ?ki2 ?patternType2 ?gp2 . ?gp2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/energy/ontology/interconnect#GraphPattern> . ?gp2 <https://www.tno.nl/energy/ontology/interconnect#hasPattern> ?pattern2 . " };

		GraphPattern gp1 = new GraphPattern(patterns[0]);
		GraphPattern gp2 = new GraphPattern(patterns[1]);

		Model m = ModelFactory.createDefaultModel();

		m.read(this.getClass().getResourceAsStream("/metadata.ttl"), null, "turtle");

		// then use the Knowledge Interaction as a query to retrieve the bindings.
		Query q = QueryFactory.create("SELECT * WHERE {" + this.convertToPattern(gp1) + "}");
		LOG.trace("Query: {}", q);
		QueryExecution qe = QueryExecutionFactory.create(q, m);
		ResultSet rs = qe.execSelect();
		BindingSet fromBindingSet = new BindingSet(rs);
		qe.close();

		BindingSet toBindingSet = GraphPatternMatcher.transformBindingSet(gp1, gp2, fromBindingSet);

		LOG.info("{}", fromBindingSet);
		LOG.info("{}", toBindingSet);

	}

	private String convertToPattern(GraphPattern gp) {

		try {

			Iterator<TriplePath> iter = gp.getGraphPattern().patternElts();

			StringBuilder sb = new StringBuilder();

			while (iter.hasNext()) {

				TriplePath tp = iter.next();
				sb.append(FmtUtils.stringForTriple(tp.asTriple(), new PrefixMappingMem()));
				sb.append(" . ");
			}

			return sb.toString();
		} catch (ParseException pe) {
			LOG.error("The graph pattern should be parseable.", pe);
		}
		return "<errorgraphpattern>";
	}

}
