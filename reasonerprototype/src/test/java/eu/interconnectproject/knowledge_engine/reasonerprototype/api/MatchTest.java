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
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Value;

public class MatchTest {

	@Test
	public void singleTripleTest() {

		Triple t = new Triple("?s type ?t");

		Triple triple = new Triple("?b type Sensor");
		Triple triple2 = new Triple("?b hasVal ?v");
		Triple triple3 = new Triple("?v type e");
		List<Triple> rhs = Arrays.asList(triple, triple2, triple3);

		Rule r = new LocalRule(null, rhs);

		Map<Triple, Map<Value, Value>> matches = r.rhsMatchesAlt(t, rhs);

		System.out.println("Matches: " + matches);
		// correct

	}

	@Test
	public void multiTripleTest2() {

		Triple t1 = new Triple("?s type ?t");
		Triple t2 = new Triple("?s hasVal ?d");
		List<Triple> obj = Arrays.asList(t1, t2);

		Triple triple = new Triple("?b type Sensor");
		Triple triple2 = new Triple("?b hasVal ?v");
		Triple triple3 = new Triple("?v type e");
		List<Triple> rhs = Arrays.asList(triple, triple2, triple3);

		Rule r = new LocalRule(null, rhs);

		List<List<Triple>> matches = r.rhsMatchesAlt(obj, rhs);

		System.out.println("Matches: " + matches);
		// correct

	}

	@Test
	public void multiTripleTest() {

		List<Triple> rhs = loadTriple("/pilot.gp");

		Triple triple1 = new Triple(
				"?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building>");
		Triple triple2 = new Triple("?c <https://saref.etsi.org/saref4bldg/hasSpace> ?z");
		Triple triple3 = new Triple(
				"?z <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace>");
		List<Triple> obj = Arrays.asList(triple1, triple3, triple2);

		Rule r = new LocalRule(null, rhs);

		List<List<Triple>> matches = r.rhsMatchesAlt(obj, rhs);

		System.out.println("Matches: " + matches);

	}

	@Test
	public void multipleTripleTest2() {
		List<Triple> rhs = Arrays.asList(new Triple("?a someProp ?b"), new Triple("?b someProp ?c"));
		List<Triple> obj = Arrays.asList(new Triple("?thing someProp ?otherThing"));

		Rule r = new LocalRule(null, null);
		List<List<Triple>> matches = r.rhsMatchesAlt(obj, rhs);

		System.out.println("Matches: " + matches);
	}

	private List<Triple> loadTriple(String aResource) {
		String gp = Util.getStringFromInputStream(MatchTest.class.getResourceAsStream(aResource));

		String[] tripleArray = gp.replace("\n", "").split(" \\.");

		List<Triple> triples = new ArrayList<Triple>();
		for (String t : tripleArray) {
			triples.add(new Triple(t.trim()));
		}
		return triples;
	}

	@Test
	public void testGPMatcher() {
		Triple t1 = new Triple("?s type ?t");
		Triple t2 = new Triple("?s hasVal ?d");
		Set<Triple> obj = new HashSet<>(Arrays.asList(t1, t2));

		Triple triple = new Triple("?b type Sensor");
		Triple triple2 = new Triple("?b hasVal ?v");
		Triple triple3 = new Triple("?v type e");
		Set<Triple> rhs = new HashSet<>(Arrays.asList(triple, triple2, triple3));

		RuleAlt r = new RuleAlt(null, rhs);

		Set<Map<Triple, Triple>> findMatchesWithConsequent = r.findMatches(obj);
		System.out.println(findMatchesWithConsequent);
	}

}
