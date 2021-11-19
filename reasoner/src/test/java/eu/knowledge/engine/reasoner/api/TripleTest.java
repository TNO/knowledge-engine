package eu.knowledge.engine.reasoner.api;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.sse.SSE;
import org.junit.Test;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class TripleTest {

	@Test
	public void tripleSubstituteTest() {
		TriplePattern t1 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern t2 = new TriplePattern("?d <type> <Sensor>");

		// if two triples matche exactly, we still need to store the matching variables,
		// otherwise we cannot detect conflicts when merging!

		Map<Node, Node> actual = t1.findMatches(t2);

		Map<Node, Node> expected = new HashMap<>();
		expected.put(SSE.parseNode("?s"), SSE.parseNode("?d"));

		assertEquals(expected, actual);

	}

}
