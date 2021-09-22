package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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
	}

}
