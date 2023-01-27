package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;

public class JenaTest {
	@Test
	public void testFloatsAndDecimals() {
		Node_Literal node1 = (Node_Literal) SSE.parseNode("\"12\"^^xsd:float");
		Node_Literal node2 = (Node_Literal) SSE.parseNode("\"12\"^^xsd:decimal");
		assertFalse(node1.matches(node2));
		assertNotEquals(node1, node2);
	}
}
