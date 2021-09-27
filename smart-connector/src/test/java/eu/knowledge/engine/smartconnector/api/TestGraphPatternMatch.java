package eu.knowledge.engine.smartconnector.api;

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
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.impl.GraphPatternMatcher;

public class TestGraphPatternMatch {

	private static final Logger LOG = LoggerFactory.getLogger(TestGraphPatternMatch.class);

	@Test
	public void testSimpleIsomorphisms() throws ParseException {

		GraphPattern gp1 = new GraphPattern("<a> <p1> <c> . <b> ?p <c> .");
		GraphPattern gp2 = new GraphPattern("<b> ?p <c> . <a> <p1> <c> .");

//		assertTrue(GraphPatternMatcher.areIsomorphic(gp1, gp2));
		Map<Integer, Integer> isomorphism = GraphPatternMatcher.getIsomorphisms(gp1, gp2);
		LOG.info("iso: {}", isomorphism);
		assertTrue(!isomorphism.isEmpty());

	}

	@Test
	public void testMultipleIsomorphisms() throws ParseException {

		String[][] positivePairs = new String[][] {

				// @formatter: off

				new String[] { "?a ?b ?c", "?x ?y ?z" }, new String[] { "<a> ?b ?c", "<a> ?y ?z" },
				// this is a known problem in the isomorphism testing. The ordering becomes
				// important if the nodes are all concrete.
				// new String[] {"<a> <p> <c> . <b> <p> <c>", "<b> <p> <c> . <a> <p> <c>"}, 
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

				},
				new String[] {
						"?obs <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/sosa/Observation> . ?obs <http://www.w3.org/ns/sosa/hasFeatureOfInterest> <https://www.tno.nl/defense/data/v1905/air> . ?obs <http://www.w3.org/ns/sosa/observedProperty> <https://www.tno.nl/defense/ontology/v1905/FahrenheitTemperature> . ?obs <http://www.w3.org/ns/sosa/hasSimpleResult> ?temp . ",
						"?obs <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/sosa/Observation> . ?obs <http://www.w3.org/ns/sosa/hasFeatureOfInterest> <https://www.tno.nl/defense/data/v1905/air> . ?obs <http://www.w3.org/ns/sosa/observedProperty> <https://www.tno.nl/defense/ontology/v1905/CelsiusTemperature> . ?obs <http://www.w3.org/ns/sosa/hasSimpleResult> ?temp . " }

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
	public void testMetadataBindingSetTransformation() {
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

		for (Binding binding : fromBindingSet) {
			assertTrue(binding.containsKey("kb1"));
			assertTrue(binding.containsKey("name1"));
			assertTrue(binding.containsKey("description1"));
			assertTrue(binding.containsKey("ki1"));
			assertTrue(binding.containsKey("kiType1"));
			assertTrue(binding.containsKey("isMeta1"));
			assertTrue(binding.containsKey("act1"));
			assertTrue(binding.containsKey("req1"));
			assertTrue(binding.containsKey("sat1"));
			assertTrue(binding.containsKey("reqType1"));
			assertTrue(binding.containsKey("satType1"));
			assertTrue(binding.containsKey("gp1"));
			assertTrue(binding.containsKey("patternType1"));
			assertTrue(binding.containsKey("pattern1"));
		}

		BindingSet toBindingSet = GraphPatternMatcher.transformBindingSet(gp1, gp2, fromBindingSet);

		for (Binding binding : toBindingSet) {
			assertTrue(binding.containsKey("kb2"));
			assertTrue(binding.containsKey("name2"));
			assertTrue(binding.containsKey("description2"));
			assertTrue(binding.containsKey("ki2"));
			assertTrue(binding.containsKey("kiType2"));
			assertTrue(binding.containsKey("isMeta2"));
			assertTrue(binding.containsKey("act2"));
			assertTrue(binding.containsKey("req2"));
			assertTrue(binding.containsKey("sat2"));
			assertTrue(binding.containsKey("reqType2"));
			assertTrue(binding.containsKey("satType2"));
			assertTrue(binding.containsKey("gp2"));
			assertTrue(binding.containsKey("patternType2"));
			assertTrue(binding.containsKey("pattern2"));
		}

		LOG.info("{}", fromBindingSet);
		LOG.info("{}", toBindingSet);

	}

	private String convertToPattern(GraphPattern gp) {
		Iterator<TriplePath> iter = gp.getGraphPattern().patternElts();

		StringBuilder sb = new StringBuilder();

		while (iter.hasNext()) {

			TriplePath tp = iter.next();
			sb.append(FmtUtils.stringForTriple(tp.asTriple(), new PrefixMappingMem()));
			sb.append(" . ");
		}

		return sb.toString();
	}

}
