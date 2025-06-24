package eu.knowledge.engine.reasoner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.BaseRule.MatchFlag;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;

@TestInstance(Lifecycle.PER_CLASS)
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

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.of(MatchFlag.ONLY_BIGGEST));
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher2() {
		TriplePattern t1 = new TriplePattern("?s <type> ?t");
		TriplePattern t2 = new TriplePattern("?s <hasVal> ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple2 = new TriplePattern("?b <hasVal> ?v");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple2));

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.of(MatchFlag.ONLY_BIGGEST));
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

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.of(MatchFlag.ONLY_BIGGEST));
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher4() {
		TriplePattern t1 = new TriplePattern("<sens1> <type> <Sensor>");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1));

		TriplePattern triple = new TriplePattern("<sens1> <type> <Sensor>");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple));

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.of(MatchFlag.ONLY_BIGGEST));

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

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.of(MatchFlag.ONLY_BIGGEST));

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

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.of(MatchFlag.ONLY_BIGGEST));
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher7() {
		TriplePattern t1 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t2 = new TriplePattern("?act <type> <CommunicativeAct>");
		TriplePattern t3 = new TriplePattern("?act <hasSatisfaction> ?sat");
		TriplePattern t4 = new TriplePattern("?sat <type> ?satType");
		TriplePattern t6 = new TriplePattern("?kb <hasDescription> ?description");
		TriplePattern t7 = new TriplePattern("?req <type> ?reqType");
		TriplePattern t8 = new TriplePattern("?ki <hasCommunicativeAct> ?act");
		TriplePattern t9 = new TriplePattern("?gp <hasPattern> ?pattern");
		TriplePattern t10 = new TriplePattern("?act <hasRequirement> ?req");
		TriplePattern t11 = new TriplePattern("?gp <type> ?patternType");
		TriplePattern t12 = new TriplePattern("?ki <isMeta> ?isMeta");
		TriplePattern t13 = new TriplePattern("?kb <hasName> ?name");
		TriplePattern t14 = new TriplePattern("?kb <hasKnowledgeInteraction> ?ki");
		TriplePattern t15 = new TriplePattern("?ki <hasGraphPattern> ?gp");
		TriplePattern t16 = new TriplePattern("?kb <type> <KnowledgeBase>");

		Set<TriplePattern> obj = new HashSet<>(
				Arrays.asList(t1, t2, t3, t4, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16));

		BaseRule r = new ProactiveRule(obj, new HashSet<>());

		Set<Match> findMatchesWithConsequent = r.antecedentMatches(obj,
				EnumSet.of(MatchFlag.ONLY_BIGGEST, MatchFlag.FULLY_COVERED));
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

		Rule r = new Rule(new HashSet<>(), tp2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(tp1, EnumSet.of(MatchFlag.ONLY_BIGGEST));
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

		Rule r = new Rule(new HashSet<>(), tp2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(tp1, EnumSet.of(MatchFlag.ONLY_BIGGEST));
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testMatchObjects() {

		TriplePattern tp1_1 = new TriplePattern("?p <type> ?t");
		TriplePattern tp1_2 = new TriplePattern("?p <hasV> ?q");

		TriplePattern tp2_1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2_2 = new TriplePattern("?s <hasV> ?val");
		TriplePattern tp2_3 = new TriplePattern("?s <type> <Device>");

		Map<TripleNode, TripleNode> mapping1 = new HashMap<TripleNode, TripleNode>();
		mapping1.put(new TripleNode(tp1_1, SSE.parseNode("?p"), 0), new TripleNode(tp2_1, SSE.parseNode("?s"), 0));
		mapping1.put(new TripleNode(tp1_1, SSE.parseNode("?t"), 2),
				new TripleNode(tp2_1, SSE.parseNode("<Sensor>"), 2));

		Match m1 = new Match(tp1_1, tp2_1, mapping1);

		Map<TripleNode, TripleNode> mapping2 = new HashMap<TripleNode, TripleNode>();
		mapping2.put(new TripleNode(tp1_2, SSE.parseNode("?p"), 0), new TripleNode(tp2_2, SSE.parseNode("?s"), 0));
		mapping2.put(new TripleNode(tp1_2, SSE.parseNode("?q"), 2), new TripleNode(tp2_2, SSE.parseNode("?val"), 2));

		Match m2 = new Match(tp1_2, tp2_2, mapping2);

		// should correctly create a combined match (because no conflict)
		Match m3 = m1.merge(m2);
		System.out.println("Match: " + m3);

		// should correctly create the same combined match as previous merge (because
		// merge should be symmetrical)
		Match m4 = m2.merge(m1);
		System.out.println("Match: " + m4);

		Map<TripleNode, TripleNode> mapping3 = new HashMap<TripleNode, TripleNode>();
		mapping3.put(new TripleNode(tp1_1, SSE.parseNode("?p"), 0), new TripleNode(tp2_3, SSE.parseNode("?s"), 0));
		mapping3.put(new TripleNode(tp1_1, SSE.parseNode("?t"), 2),
				new TripleNode(tp2_3, SSE.parseNode("<Device>"), 2));
		Match m5 = new Match(tp1_1, tp2_3, mapping3);

		// conflict, so should be null
		Match m6 = m5.merge(m1);
		System.out.println("Match: " + m6);
	}

	@Test
	public void testGPMatcher10OrderingWithinGraphPatternsShouldNotMatter() {
		TriplePattern t1 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t5 = new TriplePattern("?gp <type> ?patternType");
		TriplePattern t9 = new TriplePattern("?gp <hasPattern> ?pattern");

		TriplePattern t23 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t24 = new TriplePattern("?gp <hasPattern> ?pattern");
		TriplePattern t211 = new TriplePattern("?gp <type> ?patternType");

		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t5, t9));

		Set<TriplePattern> obj2 = new HashSet<>(Arrays.asList(t23, t24, t211));

		Rule r = new Rule(new HashSet<>(), obj);
		Rule r2 = new Rule(new HashSet<>(), obj2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj,
				EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST));
		Set<Match> findMatchesWithConsequent2 = r2.consequentMatches(obj2,
				EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST));

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
		TriplePattern t5 = new TriplePattern("?gp <type> ?patternType");
		TriplePattern t9 = new TriplePattern("?gp <hasPattern> ?pattern");

		TriplePattern t23 = new TriplePattern("?ki <type> ?kiType");
		TriplePattern t24 = new TriplePattern("?gp <hasPattern> ?pattern");
		TriplePattern t211 = new TriplePattern("?gp <type> ?patternType");

		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t5, t9));

		Set<TriplePattern> obj2 = new HashSet<>(Arrays.asList(t23, t24, t211));

		Rule r = new Rule(new HashSet<>(), obj);
		Rule r2 = new Rule(new HashSet<>(), obj2);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj,
				EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST));
		Set<Match> findMatchesWithConsequent2 = r2.consequentMatches(obj2,
				EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST));

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

		Rule r = new Rule(obj, (SinkBindingSetHandler) aBindingSet -> {
			return new CompletableFuture<Void>();
		});

		Set<Match> findMatchesWithAntecedent = r.antecedentMatches(
				new HashSet<>(Arrays.asList(/* t1, */ t5, t9, t8, t7, t6, t4, t3)),
				EnumSet.of(MatchFlag.ONLY_BIGGEST, MatchFlag.FULLY_COVERED));

		System.out.println("Size: " + findMatchesWithAntecedent.size());
//		System.out.println(findMatchesWithConsequent);

		int count = 0;
		for (Match m : findMatchesWithAntecedent) {
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
		Map<TripleNode, TripleNode> mapping1 = new HashMap<>();
		mapping1.put(new TripleNode(tp1, tp1.getSubject(), 0), new TripleNode(tp2, tp2.getSubject(), 0));
		mapping1.put(new TripleNode(tp1, tp1.getObject(), 0), new TripleNode(tp2, tp2.getObject(), 0));

		Match m1 = new Match(tp1, tp2, mapping1);

		Map<TripleNode, TripleNode> mapping2 = new HashMap<>();
		mapping2.put(new TripleNode(tp2, tp2.getSubject(), 0), new TripleNode(tp1, tp1.getSubject(), 0));
		mapping2.put(new TripleNode(tp2, tp2.getObject(), 0), new TripleNode(tp1, tp1.getObject(), 0));

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

		Map<TripleNode, TripleNode> mapping1 = new HashMap<>();
		mapping1.put(new TripleNode(tp11, tp11.getSubject(), 0), new TripleNode(tp21, tp21.getSubject(), 0));
		mapping1.put(new TripleNode(tp11, tp11.getObject(), 2), new TripleNode(tp21, tp21.getObject(), 2));
		Match m1 = new Match(tp11, tp21, mapping1);

		Map<TripleNode, TripleNode> mapping2 = new HashMap<>();
		mapping2.put(new TripleNode(tp12, tp12.getSubject(), 0), new TripleNode(tp22, tp22.getSubject(), 0));
		mapping2.put(new TripleNode(tp12, tp12.getObject(), 2), new TripleNode(tp22, tp22.getObject(), 2));
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

			Rule r = new Rule(new HashSet<>(), obj);

			Set<Match> findMatchesWithConsequent = r.consequentMatches(new HashSet<>(Arrays.asList(graphPattern)),
					EnumSet.noneOf(MatchFlag.class));

			System.out.println("graph pattern size " + gpSize + " gives matches size "
					+ findMatchesWithConsequent.size() + "-" + getNumberOfMatches(gpSize));
			assertEquals(findMatchesWithConsequent.size(), getNumberOfMatches(gpSize));
		}
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

	@Test
	public void testBugTranslate() {
		TriplePattern t1 = new TriplePattern("?s ?p ?o");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1));

		var tp7 = new TriplePattern("?a <p> ?a");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(tp7));

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.noneOf(MatchFlag.class));

		BindingSet bs = Util.toBindingSet("s=<n2>,p=<p>,o=<n3>");

		var tvbs = new TripleVarBindingSet(obj, bs);

		var nBs = tvbs.translate(rhs, findMatchesWithConsequent);

		System.out.println(nBs);
		assertTrue(nBs.isEmpty());
	}

	@Test
	public void testOtherBugTranslate() {
		TriplePattern t1 = new TriplePattern("?p <type> ?t");
		TriplePattern t2 = new TriplePattern("?p <hasValInC> ?q");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		var tp7 = new TriplePattern("?s <type> <Device>");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(tp7));

		Rule r = new Rule(new HashSet<>(), rhs);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, EnumSet.noneOf(MatchFlag.class));

		BindingSet bs = Util.toBindingSet("p=<sensor1>,q=\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>");

		var tvbs = new TripleVarBindingSet(obj, bs);

		var nBs = tvbs.translate(rhs, findMatchesWithConsequent);

		System.out.println(nBs);
		assertTrue(!nBs.isEmpty());
	}

	@Test
	public void testMatchWithReflexiveTriple() {
		// {TripleNode [tp=?x r ?x, node=?x]=TripleNode [tp=?s r a, node=a]}

		var t1 = new TriplePattern("?x <r> ?x");
		var t2 = new TriplePattern("?s <r> <a>");

		var actualMap = t1.findMatches(t2);

		Map<TripleNode, TripleNode> expectedMap = new HashMap<>();
		expectedMap.put(new TripleNode(t1, "?x", 0), new TripleNode(t2, "?s", 0));
		expectedMap.put(new TripleNode(t1, "?x", 2), new TripleNode(t2, "<a>", 2));

		assertEquals(expectedMap, actualMap);

	}

	@Test
	public void testTranslateEmptyBindingSet() {
		var t1 = new TriplePattern("?s ?p ?o");
		var t2 = new TriplePattern("?a ?b ?c");

		TripleVarBindingSet tvbs1 = new TripleVarBindingSet(new HashSet<>(Arrays.asList(t1)));

		Map<TripleNode, TripleNode> map = new HashMap<>();
		map.put(new TripleNode(t1, "?s", 0), new TripleNode(t2, "?a", 0));
		map.put(new TripleNode(t1, "?p", 1), new TripleNode(t2, "?b", 1));
		map.put(new TripleNode(t1, "?o", 2), new TripleNode(t2, "?c", 2));

		var match = new Match(t1, t2, map);

		TripleVarBindingSet tvbs2 = tvbs1
				.translate(new HashSet<>(Arrays.asList(t2)), new HashSet<>(Arrays.asList(match))).values().iterator()
				.next();

		System.out.println("BindingSet: " + tvbs2);

		assertTrue(tvbs2.isEmpty());
	}

	@Test
	public void testPloutosGPMatcher() {

		String gp = """
				?operation <type> <HarvestingOperation> .
				?operation <hasOutput> ?output .
				?operation <isOperatedOn> ?parcel .
				?parcel <contains> ?crop .
				?crop <type> ?cropType .
				?cropType <label> ?cropName .
				?parcel <hasArea> ?area .
				?farm <type> <Farm> .
				?farm <location> ?location .
				?location <lat> ?latitude .
				?location <long> ?longitude .
				?farm <hasName> ?farmName .
				?farm <contains> ?parcel .
				?farmer <managesFarm> ?farm .
				?association <type> <FarmAssociation> .
				?farmer <isMemberOf> ?association .
				?association <hasName> ?associationName .
				?parcel <hasAdministrator> ?parcelAdminName .
				?parcel <hasToponym> ?parcelToponym .
				?parcel <inRegion> ?parcelRegion .
				?output <isMeantFor> ?purpose .
				?parcel <hasCultivator> ?cultivatorName .
				?farm <hasCountry> ?country .
				?country <name> ?countryLabel .""";

		Set<TriplePattern> obj = Util.toGP(gp);

		Rule r = new Rule(obj, new SinkBindingSetHandler() {

			@Override
			public CompletableFuture<Void> handle(BindingSet aBindingSet) {
				System.out.println("bla");
				return null;
			}
		});

		System.out.println("NrOfMatches with " + obj.size() + " triple patterns: " + getNumberOfMatches(obj.size()));

		Set<Match> findMatchesWithAntecedent = r.antecedentMatches(obj,
				EnumSet.of(MatchFlag.ONLY_BIGGEST, MatchFlag.FULLY_COVERED));

		System.out.println("Size: " + findMatchesWithAntecedent.size());

	}

	@Test
	public void testPloutosGPMatcher2() {

		String gp = """
				?operation <type> <HarvestingOperation> .
				?operation <hasOutput> ?output .
				?operation <isOperatedOn> ?parcel .
				?parcel <contains> ?crop .
				?crop <type> ?cropType .
				?cropType <label> ?cropName .
				?parcel <hasArea> ?area .
				?farm <type> <Farm> .
				?farm <location> ?location .
				?location <lat> ?latitude .
				?location <long> ?longitude .
				?farm <hasName> ?farmName .
				?farm <contains> ?parcel .
				?farmer <managesFarm> ?farm .
				?association <type> <FarmAssociation> .
				?farmer <isMemberOf> ?association .
				?association <hasName> ?associationName .
				?parcel <hasAdministrator> ?parcelAdminName .
				?parcel <hasToponym> ?parcelToponym .
				?parcel <inRegion> ?parcelRegion .
				?output <isMeantFor> ?purpose .
				?parcel <hasCultivator> ?cultivatorName .
				?farm <hasCountry> ?country .
				?country <name> ?countryLabel .""";

		Set<TriplePattern> obj = Util.toGP(gp);

		String gp2 = """
				?operation2 <type> <HarvestingOperation> .
				?operation2 <hasOutput> ?output2 .
				?operation2 <isOperatedOn> ?parcel2 .
				?parcel2 <contains> ?crop2 .
				?crop2 <type> ?cropType2 .
				?cropType2 <label> ?cropName2 .
				?parcel2 <hasArea> ?area2 .
				?farm2 <type> <Farm> .
				?farm2 <location> ?location2 .
				?location2 <lat> ?latitude2 .
				?location2 <long> ?longitude2 .
				?farm2 <hasName> ?farmName2 .
				?farm2 <contains> ?parcel2 .
				?farmer2 <managesFarm> ?farm2 .
				?association2 <type> <FarmAssociation> .
				?farmer2 <isMemberOf> ?association2 .
				?association2 <hasName> ?associationName2 .
				?parcel2 <hasAdministrator> ?parcelAdminName2 .
				?parcel2 <hasToponym> ?parcelToponym2 .
				?parcel2 <inRegion> ?parcelRegion2 .
				?output2 <isMeantFor> ?purpose2 .
				?parcel2 <hasCultivator> ?cultivatorName2 .
				?farm2 <hasCountry> ?country2 .
				?country2 <name> ?countryLabel2 .""";

		Set<TriplePattern> obj2 = Util.toGP(gp2);

		BaseRule r1 = new ProactiveRule(obj, new HashSet<>());

		BaseRule r2 = new Rule(new HashSet<>(), obj2);

		System.out.println("NrOfMatches with " + obj.size() + " triple patterns: " + getNumberOfMatches(obj.size()));

		var findMatchesWithConsequent = BaseRule.getMatches(r1, new HashSet<>(Arrays.asList(r2)), true,
				EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST, MatchFlag.FULLY_COVERED));

		System.out.println("Size: " + findMatchesWithConsequent.size());
		assertEquals(1, findMatchesWithConsequent.size());
	}

	@Test
	public void testNewGPMatcher1() {
		TriplePattern tp1_1 = new TriplePattern("?p <type> ?t");
		TriplePattern tp1_2 = new TriplePattern("?p <hasV> ?q");
		Set<TriplePattern> tp1 = new HashSet<>(Arrays.asList(tp1_1, tp1_2));

		TriplePattern tp2_1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp2_2 = new TriplePattern("?s <hasV> ?val");
		TriplePattern tp2_3 = new TriplePattern("?s <type> <Device>");
		Set<TriplePattern> tp2 = new HashSet<>(Arrays.asList(tp2_1, tp2_2, tp2_3));

		BaseRule r1 = new ProactiveRule(tp1, new HashSet<>());

		BaseRule r2 = new Rule(new HashSet<>(), tp2);

		var matches = BaseRule.getMatches(r1, new HashSet<>(Arrays.asList(r2)), true,
				EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST));

		System.out.println(matches);

		assertEquals(matches.size(), 1);
	}

	@Test
	public void testNewGPMatcherTransitivity() {
		TriplePattern tp1_1 = new TriplePattern("?x <hasAncestor> ?y");
		TriplePattern tp1_2 = new TriplePattern("?y <hasAncestor> ?z");
		Set<TriplePattern> tp1 = new HashSet<>(Arrays.asList(tp1_1, tp1_2));

		TriplePattern tp2_1 = new TriplePattern("?a <hasAncestor> ?b");
		Set<TriplePattern> tp2 = new HashSet<>(Arrays.asList(tp2_1));

		BaseRule r1 = new ProactiveRule(tp1, new HashSet<>());

		BaseRule r2 = new Rule(new HashSet<>(), tp2);

		var matches = BaseRule.getMatches(r1, new HashSet<>(Arrays.asList(r2)), true, EnumSet.noneOf(MatchFlag.class));

		System.out.println(matches);
		assertEquals(1, matches.size());

	}

	@Test
	public void testNewGPMatcher2() {
		TriplePattern tp1_1 = new TriplePattern("?x <hasP1> ?y");
		TriplePattern tp1_2 = new TriplePattern("?y <hasP2> ?z");
		TriplePattern tp1_3 = new TriplePattern("?z <hasP1> ?w");
		Set<TriplePattern> tp1 = new HashSet<>(Arrays.asList(tp1_1, tp1_2, tp1_3));

		TriplePattern tp2_1 = new TriplePattern("?a ?b ?c");
		Set<TriplePattern> tp2 = new HashSet<>(Arrays.asList(tp2_1));

		BaseRule r1 = new ProactiveRule(tp1, new HashSet<>());

		BaseRule r2 = new Rule(new HashSet<>(), tp2);

		var matches = BaseRule.getMatches(r1, new HashSet<>(Arrays.asList(r2)), true, EnumSet.noneOf(MatchFlag.class));

		System.out.println(matches);
		assertEquals(1, matches.size());

	}
}
