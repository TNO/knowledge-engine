package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;

public class JenaTest {
	@Test
	public void testFloatsAndDecimals() {
		Node node1 = (Node) SSE.parseNode("\"12\"^^xsd:float");
		Node node2 = (Node) SSE.parseNode("\"12\"^^xsd:decimal");
		assertFalse(node1.matches(node2));
		assertNotEquals(node1, node2);
	}
}
