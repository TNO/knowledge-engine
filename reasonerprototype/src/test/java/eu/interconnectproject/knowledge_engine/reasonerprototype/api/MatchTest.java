package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import eu.interconnectproject.knowledge_engine.reasonerprototype.LocalRule;
import eu.interconnectproject.knowledge_engine.reasonerprototype.Rule;
import eu.interconnectproject.knowledge_engine.reasonerprototype.RuleAlt;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.TriplePattern.Value;

public class MatchTest {

	@Test
	public void singleTripleTest() {

		TriplePattern t = new TriplePattern("?s type ?t");

		TriplePattern triple = new TriplePattern("?b type Sensor");
		TriplePattern triple2 = new TriplePattern("?b hasVal ?v");
		TriplePattern triple3 = new TriplePattern("?v type e");
		List<TriplePattern> rhs = Arrays.asList(triple, triple2, triple3);

		Rule r = new LocalRule(null, rhs);

		Map<TriplePattern, Map<Value, Value>> matches = r.rhsMatchesAlt(t, rhs);

		System.out.println("Matches: " + matches);
		// correct

	}

	@Test
	public void multiTripleTest2() {

		TriplePattern t1 = new TriplePattern("?s type ?t");
		TriplePattern t2 = new TriplePattern("?s hasVal ?d");
		List<TriplePattern> obj = Arrays.asList(t1, t2);

		TriplePattern triple = new TriplePattern("?b type Sensor");
		TriplePattern triple2 = new TriplePattern("?b hasVal ?v");
		TriplePattern triple3 = new TriplePattern("?v type e");
		List<TriplePattern> rhs = Arrays.asList(triple, triple2, triple3);

		Rule r = new LocalRule(null, rhs);

		List<List<TriplePattern>> matches = r.rhsMatchesAlt(obj, rhs);

		System.out.println("Matches: " + matches);
		// correct

	}

	@Test
	public void multiTripleTest() {

		List<TriplePattern> rhs = loadTriple("/pilot.gp");

		TriplePattern triple1 = new TriplePattern(
				"?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building>");
		TriplePattern triple2 = new TriplePattern("?c <https://saref.etsi.org/saref4bldg/hasSpace> ?z");
		TriplePattern triple3 = new TriplePattern(
				"?z <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace>");
		List<TriplePattern> obj = Arrays.asList(triple1, triple3, triple2);

		Rule r = new LocalRule(null, rhs);

		List<List<TriplePattern>> matches = r.rhsMatchesAlt(obj, rhs);

		System.out.println("Matches: " + matches);

	}

	@Test
	public void multipleTripleTest2() {
		List<TriplePattern> rhs = Arrays.asList(new TriplePattern("?a someProp ?b"), new TriplePattern("?b someProp ?c"));
		List<TriplePattern> obj = Arrays.asList(new TriplePattern("?thing someProp ?otherThing"));

		Rule r = new LocalRule(null, null);
		List<List<TriplePattern>> matches = r.rhsMatchesAlt(obj, rhs);

		System.out.println("Matches: " + matches);
	}

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
		TriplePattern t1 = new TriplePattern("?s type ?t");
		TriplePattern t2 = new TriplePattern("?s hasVal ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple = new TriplePattern("?b type Sensor");
		TriplePattern triple2 = new TriplePattern("?b hasVal ?v");
		TriplePattern triple3 = new TriplePattern("?v type e");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple, triple2, triple3));

		RuleAlt r = new RuleAlt(null, rhs);

		Set<Map<TriplePattern, TriplePattern>> findMatchesWithConsequent = r.consequentMatches(obj);
		System.out.println(findMatchesWithConsequent);
	}

	@Test
	public void testGPMatcher2() {
		TriplePattern t1 = new TriplePattern("?s type ?t");
		TriplePattern t2 = new TriplePattern("?s hasVal ?d");
		Set<TriplePattern> obj = new HashSet<>(Arrays.asList(t1, t2));

		TriplePattern triple2 = new TriplePattern("?b hasVal ?v");
		Set<TriplePattern> rhs = new HashSet<>(Arrays.asList(triple2));

		RuleAlt r = new RuleAlt(null, rhs);

		Set<Map<TriplePattern, TriplePattern>> findMatchesWithConsequent = r.consequentMatches(obj);
		System.out.println(findMatchesWithConsequent);
	}

}
