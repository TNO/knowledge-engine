package eu.knowledge.engine.reasoner.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.sse.SSE;
import org.junit.Test;

import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Rule.MatchStrategy;

public class MatchTest {

	private List<TriplePattern> loadTriple(String aResource) {
		String gp = Util.getStringFromInputStream(MatchTest.class.getResourceAsStream(aResource));

		String[] tripleArray = gp.replace("\n", "").split(" \\.");

		List<TriplePattern> triples = new ArrayList<TriplePattern>();
		for (String t : tripleArray) {
			triples.add(new TriplePattern(t.trim()));
		}
		return triples;
	}

	@Test
	public void testGPMatcher() {
		TriplePattern t1 = new TriplePattern("?s <type> ?t");
		TriplePattern t2 = new TriplePattern("?s <hasVal> ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple = new TriplePattern("?b <type> <Sensor>");
		TriplePattern triple2 = new TriplePattern("?b <hasVal> ?v");
		TriplePattern triple3 = new TriplePattern("?v <type> <e>");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple, triple2, triple3));

		Rule r = new Rule(null, rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher2() {
		TriplePattern t1 = new TriplePattern("?s <type> ?t");
		TriplePattern t2 = new TriplePattern("?s <hasVal> ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple2 = new TriplePattern("?b <hasVal> ?v");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple2));

		Rule r = new Rule(null, rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher3() {
		TriplePattern t1 = new TriplePattern("?s <type> ?t");
		TriplePattern t2 = new TriplePattern("?s <hasVal> ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple = new TriplePattern("?b <type> <Sensor>");
		TriplePattern triple2 = new TriplePattern("?b <hasVal> ?v");
		TriplePattern triple3 = new TriplePattern("?v <type> <e>");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple, triple2, triple3));

		Rule r = new Rule(null, rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher4() {
		TriplePattern t1 = new TriplePattern("<sens1> <type> <Sensor>");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1));

		TriplePattern triple = new TriplePattern("<sens1> <type> <Sensor>");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple));

		Rule r = new Rule(null, rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);

		// there should be a match, but its mapping should be empty nothing needs to
		// happen to translate one to the other.
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher5() {
		TriplePattern t1 = new TriplePattern("?s ?p ?o");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1));

		TriplePattern triple = new TriplePattern("?s ?p ?o");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple));

		Rule r = new Rule(null, rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);

		// there should be a match and its mapping should be empty because nothing needs
		// to happen to translate one to the other.
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher6() {
		TriplePattern t1 = new TriplePattern("?s <type> ?t");
		TriplePattern t2 = new TriplePattern("?s <hasVal> ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple = new TriplePattern("?b <type> <Sensor>");
		TriplePattern triple2 = new TriplePattern("?b <hasVal> ?v");
		TriplePattern triple3 = new TriplePattern("?b <type> <Device>");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple, triple2, triple3));

		Rule r = new Rule(null, rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher7() {
		TriplePattern t1 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t2 = new TriplePattern("?act <type> <CommunicativeAct>");
		TriplePattern t3 = new TriplePattern("?act <hasSatisfaction> ?sat");
		TriplePattern t4 = new TriplePattern("?sat <type> ?satType");
		TriplePattern t5 = new TriplePattern("?ki ?patternType ?gp");
		TriplePattern t6 = new TriplePattern("?kb <hasDescription> ?description");
		TriplePattern t7 = new TriplePattern("?req <type> ?reqType");
		TriplePattern t8 = new TriplePattern("?ki <hasCommunicativeAct> ?act");
		TriplePattern t9 = new TriplePattern("?gp <hasPattern> ?pattern");
		TriplePattern t10 = new TriplePattern("?act <hasRequirement> ?req");
		TriplePattern t11 = new TriplePattern("?gp <type> <GraphPattern>");
		TriplePattern t12 = new TriplePattern("?ki <isMeta> ?isMeta");
		TriplePattern t13 = new TriplePattern("?kb <hasName> ?name");
		TriplePattern t14 = new TriplePattern("?kb <hasKnowledgeInteraction> ?ki");
		TriplePattern t15 = new TriplePattern("?ki <hasGraphPattern> ?gp");
		TriplePattern t16 = new TriplePattern("?kb <type> <KnowledgeBase>");

		Set<TriplePattern> obj = new HashSet<>(
				Arrays.asList(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16));

		Rule r = new Rule(null, obj);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ONLY_FULL_MATCHES);
		System.out.println("Size: " + findMatchesWithConsequent.size());

		for (Match m : findMatchesWithConsequent) {
			System.out.println(m.getMappings());
		}

	}

	@Test
	public void testGPMatcher8() {
		TriplePattern tp1_1 = new TriplePattern("?p <type> ?t");
		TriplePattern tp1_2 = new TriplePattern("?p <hasV> ?q");
		Set<TriplePattern> tp1 = new HashSet<>(Arrays.asList(tp1_1, tp1_2));

		TriplePattern tp2_1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2_2 = new TriplePattern("?s <hasV> ?val");
		TriplePattern tp2_3 = new TriplePattern("?s <type> <Device>");
		Set<TriplePattern> tp2 = new HashSet<>(Arrays.asList(tp2_1, tp2_2, tp2_3));

		Rule r = new Rule(null, tp2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(tp1, MatchStrategy.FIND_ALL_MATCHES);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher9() {
		TriplePattern tp1_1 = new TriplePattern("?p <type> ?t");
		TriplePattern tp1_2 = new TriplePattern("?p <hasV> ?q");
		TriplePattern tp1_3 = new TriplePattern("?p <hasV2> ?q2");
		Set<TriplePattern> tp1 = new HashSet<>(Arrays.asList(tp1_1, tp1_2, tp1_3));

		TriplePattern tp2_1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2_2 = new TriplePattern("?s <hasV> ?val");
		TriplePattern tp2_3 = new TriplePattern("?s <type> <Device>");
		Set<TriplePattern> tp2 = new HashSet<>(Arrays.asList(tp2_1, tp2_2, tp2_3));

		Rule r = new Rule(null, tp2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(tp1, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testMatchObjects() {

		TriplePattern tp1_1 = new TriplePattern("?p <type> ?t");
		TriplePattern tp1_2 = new TriplePattern("?p <hasV> ?q");

		TriplePattern tp2_1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2_2 = new TriplePattern("?s <hasV> ?val");
		TriplePattern tp2_3 = new TriplePattern("?s <type> <Device>");

		Map<Node, Node> mapping1 = new HashMap<Node, Node>();
		mapping1.put(SSE.parseNode("?p"), SSE.parseNode("?s"));
		mapping1.put(SSE.parseNode("?t"), SSE.parseNode("<Sensor>"));

		Match m1 = new Match(tp1_1, tp2_1, mapping1);

		Map<Node, Node> mapping2 = new HashMap<Node, Node>();
		mapping2.put(SSE.parseNode("?p"), SSE.parseNode("?s"));
		mapping2.put(SSE.parseNode("?q"), SSE.parseNode("?val"));

		Match m2 = new Match(tp1_2, tp2_2, mapping2);

		// should correctly create a combined match (because no conflict)
		Match m3 = m1.merge(m2);
		System.out.println("Match: " + m3);

		// should correctly create the same combined match as previous merge (because
		// merge should be symmetrical)
		Match m4 = m2.merge(m1);
		System.out.println("Match: " + m4);

		Map<Node, Node> mapping3 = new HashMap<Node, Node>();
		mapping3.put(SSE.parseNode("?p"), SSE.parseNode("?s"));
		mapping3.put(SSE.parseNode("?t"), SSE.parseNode("<Device>"));
		Match m5 = new Match(tp1_1, tp2_3, mapping3);

		// conflict, so should be null
		Match m6 = m5.merge(m1);
		System.out.println("Match: " + m6);
	}

	@Test
	public void testGPMatcher10OrderingWithinGraphPatternsShouldNotMatter() {
		TriplePattern t1 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t5 = new TriplePattern("?ki ?patternType ?gp");
		TriplePattern t9 = new TriplePattern("?gp <hasPattern> ?pattern");

		TriplePattern t23 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t24 = new TriplePattern("?gp <hasPattern> ?pattern");
		TriplePattern t211 = new TriplePattern("?ki ?patternType ?gp");

		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t5, t9));

		Set<TriplePattern> obj2 = new HashSet<>(Arrays.asList(t23, t24, t211));

		Rule r = new Rule(null, obj);
		Rule r2 = new Rule(null, obj2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ONLY_FULL_MATCHES);
		Set<Match> findMatchesWithConsequent2 = r2.consequentMatches(obj2, MatchStrategy.FIND_ONLY_FULL_MATCHES);

		System.out.println("Size 1: " + findMatchesWithConsequent.size());
		System.out.println(findMatchesWithConsequent);
		System.out.println("Size 2: " + findMatchesWithConsequent2.size());
		System.out.println(findMatchesWithConsequent2);

		assertTrue(findMatchesWithConsequent.size() == findMatchesWithConsequent2.size());

		for (Match m : findMatchesWithConsequent) {
			System.out.println(m.getMappings());
		}

	}

	@Test
	public void testGPMatcher11VariableNamesMatter() {
		TriplePattern t1 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t5 = new TriplePattern("?ki ?patternType ?gp");
		TriplePattern t9 = new TriplePattern("?gp <hasPattern> ?pattern");

		TriplePattern t23 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t24 = new TriplePattern("?gp <hasPattern> ?pattern");
		TriplePattern t211 = new TriplePattern("?ki ?patternType ?gp");

		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t5, t9));

		Set<TriplePattern> obj2 = new HashSet<>(Arrays.asList(t23, t24, t211));

		Rule r = new Rule(null, obj);
		Rule r2 = new Rule(null, obj2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ONLY_FULL_MATCHES);
		Set<Match> findMatchesWithConsequent2 = r2.consequentMatches(obj2, MatchStrategy.FIND_ONLY_FULL_MATCHES);

		System.out.println("Size 1: " + findMatchesWithConsequent.size());
		System.out.println(findMatchesWithConsequent);
		System.out.println("Size 2: " + findMatchesWithConsequent2.size());
		System.out.println(findMatchesWithConsequent2);

		assertTrue(findMatchesWithConsequent.size() == findMatchesWithConsequent2.size());

		for (Match m : findMatchesWithConsequent) {
			System.out.println(m.getMappings());
		}

	}

	@Test
	public void testGPMatcher12() {
		TriplePattern t1 = new TriplePattern("?a ?b ?c");
		TriplePattern t5 = new TriplePattern("?d ?e ?f");
		TriplePattern t9 = new TriplePattern("?g ?h ?i");
		TriplePattern t8 = new TriplePattern("?x ?y ?z");
		TriplePattern t7 = new TriplePattern("?u ?w ?v");
		TriplePattern t6 = new TriplePattern("?j ?k ?l");
		TriplePattern t4 = new TriplePattern("?m ?n ?o");
		TriplePattern t3 = new TriplePattern("?p ?q ?r");

		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(/* t1, */ t5, t9, t8, t7, t6, t4, t3));

		Rule r = new Rule(null, obj);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(
				new HashSet<>(Arrays.asList(/* t1, */ t5, t9, t8, t7, t6, t4, t3)), MatchStrategy.FIND_ALL_MATCHES);

		System.out.println("Size: " + findMatchesWithConsequent.size());
//		System.out.println(findMatchesWithConsequent);

		int count = 0;
		for (Match m : findMatchesWithConsequent) {
//			System.out.println(m.getMatchingPatterns());

			if (m.getMatchingPatterns().size() == 3) {
				count++;
			}
		}
//		System.out.println("Number of 3 size matches: " + count);

	}

	@Test
	public void testEqualityOfMatches1() {
		TriplePattern tp1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2 = new TriplePattern("?p <type> ?t");
		Map<Node, Node> mapping1 = new HashMap<>();
		mapping1.put(tp1.getSubject(), tp2.getSubject());
		mapping1.put(tp1.getObject(), tp2.getObject());

		Match m1 = new Match(tp1, tp2, mapping1);

		Map<Node, Node> mapping2 = new HashMap<>();
		mapping2.put(tp2.getSubject(), tp1.getSubject());
		mapping2.put(tp2.getObject(), tp1.getObject());

		Match m2 = new Match(tp2, tp1, mapping2);

		assertTrue(m1.equals(m1));
		assertTrue(m2.equals(m2));
		assertFalse(m1.equals(m2));
		assertFalse(m2.equals(m1));
	}

	@Test
	public void testEqualityOfMatches2() {
		TriplePattern tp11 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?s <hasVal> ?v");

		TriplePattern tp21 = new TriplePattern("?p <type> ?t");
		TriplePattern tp22 = new TriplePattern("?p <hasVal> ?val");

		Map<Node, Node> mapping1 = new HashMap<>();
		mapping1.put(tp11.getSubject(), tp21.getSubject());
		mapping1.put(tp11.getObject(), tp21.getObject());
		Match m1 = new Match(tp11, tp21, mapping1);

		Map<Node, Node> mapping2 = new HashMap<>();
		mapping2.put(tp12.getSubject(), tp22.getSubject());
		mapping2.put(tp12.getObject(), tp22.getObject());
		Match m2 = new Match(tp12, tp22, mapping2);

		assertTrue(m1.equals(m1));
		assertTrue(m2.equals(m2));
		assertFalse(m1.equals(m1.inverse()));
		assertFalse(m2.equals(m2.inverse()));

		assertFalse(m1.equals(m2));
		assertFalse(m2.equals(m1));

		assertTrue(m1.merge(m2).equals(m2.merge(m1)));
		assertTrue(m1.merge(m2).inverse().equals(m2.merge(m1).inverse()));
		assertFalse(m1.merge(m2).equals(m2.merge(m1).inverse()));
		assertFalse(m1.merge(m2).inverse().equals(m2.merge(m1)));

	}

	@Test
	public void testGPMatcherCardinalityTest() {

		for (int gpSize = 1; gpSize < 7; gpSize++) {

			TriplePattern[] graphPattern = new TriplePattern[gpSize];

			for (int i = 0; i < gpSize; i++) {
				graphPattern[i] = new TriplePattern("?a" + (i + 1) + " ?b" + (i + 1) + " ?c" + (i + 1));
			}

			Set<TriplePattern> obj = new HashSet<>(Arrays.asList(graphPattern));

			Rule r = new Rule(null, obj);

			Set<Match> findMatchesWithConsequent = r.consequentMatches(new HashSet<>(Arrays.asList(graphPattern)),
					MatchStrategy.FIND_ALL_MATCHES);

			System.out.println("graph pattern size " + gpSize + " gives matches size "
					+ findMatchesWithConsequent.size() + "-" + getNumberOfMatches(gpSize));
			assertEquals(findMatchesWithConsequent.size(), getNumberOfMatches(gpSize));
		}
	}

	@Test
	public void testNewMatchingAlgorithm() {

		Set<TriplePattern> antecedent = new HashSet<TriplePattern>(Arrays.asList(new TriplePattern("?s <p1> ?o1"),
				new TriplePattern("?s <p2> ?o2"), new TriplePattern("?s <p3> ?o3"), new TriplePattern("?s <p4> ?o4")));

		Set<TriplePattern> rule1consequent = new HashSet<TriplePattern>(
				Arrays.asList(new TriplePattern("?a <p1> ?b1"), new TriplePattern("?a <p2> ?b2")));

		Rule rule1 = new Rule(null, rule1consequent);

		Set<TriplePattern> rule2consequent = new HashSet<TriplePattern>(
				Arrays.asList(new TriplePattern("?v <p2> ?w2"), new TriplePattern("?v <p3> ?w3")));
		Rule rule2 = new Rule(null, rule2consequent);

		Set<Rule> allRules = new HashSet<>(Arrays.asList(rule1, rule2));

		Set<Map<Rule, Match>> completeMatches = Rule.findMatches(antecedent, allRules,
				MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, true);

		System.out.println("Matches: " + completeMatches);
	}

	private long getNumberOfMatches(int graphPatternSize) {

		int sum = 0;
		for (int k = 1; k <= graphPatternSize; k++) {

			sum += binomial(graphPatternSize, k).longValue()
					* (factorial(BigInteger.valueOf(graphPatternSize)).longValue()
							/ factorial(BigInteger.valueOf(graphPatternSize - k)).longValue());

		}

		return sum;
	}

	public static BigInteger factorial(BigInteger number) {
		BigInteger result = BigInteger.valueOf(1);

		for (long factor = 2; factor <= number.longValue(); factor++) {
			result = result.multiply(BigInteger.valueOf(factor));
		}

		return result;
	}

	static BigInteger binomial(final int N, final int K) {
		BigInteger ret = BigInteger.ONE;
		for (int k = 0; k < K; k++) {
			ret = ret.multiply(BigInteger.valueOf(N - k)).divide(BigInteger.valueOf(k + 1));
		}
		return ret;
	}
}
