package eu.knowledge.engine.reasoner.api;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TriplePattern.Value;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;

public class TripleTest {

	@Test
	public void tripleSubstituteTest() {
		TriplePattern t1 = new TriplePattern("?s type Sensor");
		TriplePattern t2 = new TriplePattern("?s type Sensor");

		// if two triples matche exactly, we still need to store the matching variables,
		// otherwise we cannot detect conflicts when merging!

		Map<Value, Value> actual = t1.findMatches(t2);

		Map<Value, Value> expected = new HashMap<>();
		expected.put(new Variable("?s"), new Variable("?s"));

		assertEquals(expected, actual);

	}

}
