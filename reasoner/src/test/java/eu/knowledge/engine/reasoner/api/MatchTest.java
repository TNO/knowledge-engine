package eu.knowledge.engine.reasoner.api;

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
		TriplePattern t2 = new TriplePattern("?act <type> <CommunicativeAct>");
		TriplePattern t3 = new TriplePattern("?act <hasSatisfaction> ?sat");
		TriplePattern t6 = new TriplePattern("?kb <hasDescription> ?description");
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
				Arrays.asList(t2, t3, t6, t8, t9, t10, t11, t12, t13, t14, t15, t16 /* , t17, t18 */));

		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(t13, t16));

		Rule r = new Rule(null, obj);

		Set<Match> findMatchesWithConsequent = r.consequentMatches(obj, MatchStrategy.FIND_ALL_MATCHES);
		System.out.println("Size: " + findMatchesWithConsequent.size());
//		System.out.println(findMatchesWithConsequent);
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

}
