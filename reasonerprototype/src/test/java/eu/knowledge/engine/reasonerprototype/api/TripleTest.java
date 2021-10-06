package eu.knowledge.engine.reasonerprototype.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Value;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

public class TripleTest {

	@Test
	public void test() {
		TriplePattern t1, t2;
		Binding b;
		Map<TriplePattern.Value, TriplePattern.Value> expected;

		t1 = new TriplePattern("?s p o");
		t2 = new TriplePattern("?sub p o");
		assertTrue(t1.matches(t2, new Binding()));
		expected = new HashMap<>();
		expected.put(new TriplePattern.Variable("?s"), new TriplePattern.Variable("?sub"));
		assertEquals(expected, t1.matchesWithSubstitutionMap(t2));

		t1 = new TriplePattern("s p o");
		t2 = new TriplePattern("?sub p o");
		assertTrue(t1.matches(t2, new Binding()));
		expected = new HashMap<>();
		expected.put(new TriplePattern.Literal("s"), new TriplePattern.Variable("?sub"));
		assertEquals(expected, t1.matchesWithSubstitutionMap(t2));

		assertEquals(expected, t1.matchesWithSubstitutionMap(t2));
	}

	@Test
	public void tripleSubstituteTest() {
		TriplePattern t1 = new TriplePattern("?s type Sensor");
		TriplePattern t2 = new TriplePattern("?s type Sensor");

		// if two triples matche exactly, we still need to store the matching variables,
		// otherwise we cannot detect conflicts when merging!

		Map<Value, Value> actual = t1.matchesWithSubstitutionMap(t2);

		Map<Value, Value> expected = new HashMap<>();
		expected.put(new Variable("?s"), new Variable("?s"));

		assertEquals(expected, actual);

	}

}
