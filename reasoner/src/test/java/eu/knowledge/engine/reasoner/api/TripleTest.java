package eu.knowledge.engine.reasoner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;

public class TripleTest {

	@Test
	public void tripleSubstituteTest() {
		TriplePattern t1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern t2 = new TriplePattern("?d <type> <Sensor>");

		// if two triples matche exactly, we still need to store the matching variables,
		// otherwise we cannot detect conflicts when merging!

		Map<TripleNode, TripleNode> actual = t1.findMatches(t2);

		Map<TripleNode, TripleNode> expected = new HashMap<>();
		expected.put(new TripleNode(t1, SSE.parseNode("?s"), 0), new TripleNode(t2, SSE.parseNode("?d"), 0));

		assertEquals(expected, actual);

	}

}
