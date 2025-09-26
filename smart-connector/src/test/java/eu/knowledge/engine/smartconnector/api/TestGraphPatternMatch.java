package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.lang.arq.javacc.ParseException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.BaseRule.CombiMatch;
import eu.knowledge.engine.smartconnector.impl.Util;

public class TestGraphPatternMatch {

	private static final Logger LOG = LoggerFactory.getLogger(TestGraphPatternMatch.class);

	@Test
	public void testSimpleIsomorphisms() throws ParseException {

		GraphPattern gp1 = new GraphPattern("<a> <p1> <c> . <b> ?p <c> .");
		GraphPattern gp2 = new GraphPattern("<b> ?p <c> . <a> <p1> <c> .");

		var isomorphism = getIsos(gp1, gp2);
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
						"?kb1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> . ?kb1 <https://w3id.org/knowledge-engine/hasName> ?name1 . ?kb1 <https://w3id.org/knowledge-engine/hasDescription> ?description . ?kb1 <https://w3id.org/knowledge-engine/hasKnowledgeInteraction> ?ki1 . ?ki1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?kiType1 . ?ki1 <https://w3id.org/knowledge-engine/isMeta> ?isMeta1 . ?ki1 <https://w3id.org/knowledge-engine/hasCommunicativeAct> ?act1 . ?act1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/CommunicativeAct> . ?act1 <https://w3id.org/knowledge-engine/hasRequirement> ?req1 . ?act1 <https://w3id.org/knowledge-engine/hasSatisfaction> ?sat1 . ?req1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?reqType1 . ?sat1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?satType1 . ?ki1 <https://w3id.org/knowledge-engine/hasGraphPattern> ?gp1 . ?gp1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?patternType1 . ?gp1 <https://w3id.org/knowledge-engine/hasPattern> ?pattern1 . ",
						"?kb2 <https://w3id.org/knowledge-engine/hasName> ?name2 . ?kb2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/KnowledgeBase> . ?kb2 <https://w3id.org/knowledge-engine/hasDescription> ?description . ?kb2 <https://w3id.org/knowledge-engine/hasKnowledgeInteraction> ?ki2 . ?ki2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?kiType2 . ?ki2 <https://w3id.org/knowledge-engine/isMeta> ?isMeta2 . ?ki2 <https://w3id.org/knowledge-engine/hasCommunicativeAct> ?act2 . ?act2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://w3id.org/knowledge-engine/CommunicativeAct> . ?act2 <https://w3id.org/knowledge-engine/hasRequirement> ?req2 . ?act2 <https://w3id.org/knowledge-engine/hasSatisfaction> ?sat2 . ?req2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?reqType2 . ?sat2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?satType2 . ?ki2 <https://w3id.org/knowledge-engine/hasGraphPattern> ?gp2 . ?gp2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?patternType2 . ?gp2 <https://w3id.org/knowledge-engine/hasPattern> ?pattern2 . " }

				// @formatter: on
		};

		for (String[] pair : positivePairs) {
			GraphPattern ks1 = new GraphPattern(pair[0]);
			GraphPattern ks2 = new GraphPattern(pair[1]);

			boolean doesMatch = !getIsos(ks1, ks2).isEmpty();
			LOG.info("Should be equal: {}", doesMatch);
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

			boolean doesMatch = !this.getIsos(ks1, ks2).isEmpty();

			LOG.info("Should not be equal: {}", doesMatch);
			assertFalse(doesMatch);

			LOG.info("--------------------");
		}
	}

	@Test
	public void testGraphPatternWithLiteral() {
		GraphPattern gp = new GraphPattern("?s <http://example.org/something> \"test\" .");
		assertTrue(true);
	}

	private Set<Match> getIsos(GraphPattern gp1, GraphPattern gp2) {

		Rule anteRule = new Rule(Util.translateGraphPatternTo(gp1),
				(SinkBindingSetHandler) new SinkBindingSetHandler() {

					@Override
					public CompletableFuture<Void> handle(eu.knowledge.engine.reasoner.api.BindingSet aBindingSet) {
						// TODO Auto-generated method stub
						return null;
					}
				});

		Rule consRule = new Rule(Util.translateGraphPatternTo(gp2), new TransformBindingSetHandler() {

			@Override
			public CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> handle(
					eu.knowledge.engine.reasoner.api.BindingSet bs) {
				// TODO Auto-generated method stub
				return null;
			}
		});

//		assertTrue(GraphPatternMatcher.areIsomorphic(gp1, gp2));
		Map<BaseRule, Set<Match>> isomorphism = convertToMapping(BaseRule.getMatches(anteRule,
				Collections.singleton(consRule), true, MatchStrategy.ENTRY_LEVEL.toConfig(true)));

		return isomorphism.containsKey(consRule) ? isomorphism.get(consRule) : new HashSet<Match>();
	}

	// copied from RuleStore#convertToMapping
	private Map<BaseRule, Set<Match>> convertToMapping(Set<CombiMatch> someMatches) {
		var mapping = new HashMap<BaseRule, Set<Match>>();

		for (CombiMatch cm : someMatches) {
			for (Map.Entry<BaseRule, Set<Match>> entry : cm.entrySet()) {
				// get rule match set
				var ruleMatchSet = mapping.get(entry.getKey());
				if (ruleMatchSet == null) {
					ruleMatchSet = new HashSet<Match>();
					mapping.put(entry.getKey(), ruleMatchSet);
				}
				ruleMatchSet.addAll(entry.getValue());
			}
		}
		return mapping;
	}

}
