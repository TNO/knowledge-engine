package eu.interconnectproject.knowledge_engine.reasonerprototype.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TripleTest {

	@Test
	public void test() {
		Triple t1, t2;
		Binding b;
		Map<Triple.Value, Triple.Value> expected;

		t1 = new Triple("?s p o");
		t2 = new Triple("?sub p o");
		assertTrue(t1.matches(t2, new Binding()));
		expected = new HashMap<>();
		expected.put(new Triple.Variable("?s"), new Triple.Variable("?sub"));
		assertEquals(expected, t1.matchesWithSubstitutionMap(t2));

		t1 = new Triple("s p o");
		t2 = new Triple("?sub p o");
		assertTrue(t1.matches(t2, new Binding()));
		expected = new HashMap<>();
		expected.put(new Triple.Literal("s"), new Triple.Variable("?sub"));
		assertEquals(expected, t1.matchesWithSubstitutionMap(t2));
	}

}
