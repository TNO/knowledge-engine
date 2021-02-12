package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.util.Comparator;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.jgrapht.Graph;
import org.jgrapht.alg.isomorphism.IsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;

public class GraphPatternMatcher {

	private static final Logger LOG = LoggerFactory.getLogger(GraphPatternMatcher.class);

	private static VertexComparator vertexComparator = new VertexComparator();
	private static EdgeComparator edgeComparator = new EdgeComparator();

	/**
	 * Checks if the knowledge represented by this graph pattern is isomorph to the
	 * given knowledge. In other words, variables and ordering of the triple
	 * patterns are ignored when determining isomorphisms.
	 * 
	 * @param someKnowledge The knowledge to find an isomorphism with from this
	 *                      knowledge.
	 * @return Whether the given knowledge is isomorph with this knowledge.
	 * @throws ParseException
	 */
	public static boolean checkIsomorph(GraphPattern gp1, GraphPattern gp2) {

		boolean isomorph = false;
		try {
			Graph<Node, NodeEdge> g1 = convertToJGraph(gp1.getGraphPattern());
			Graph<Node, NodeEdge> g2 = convertToJGraph(gp2.getGraphPattern());
			IsomorphismInspector<Node, NodeEdge> inspect = new VF2GraphIsomorphismInspector<Node, NodeEdge>(g1, g2,
					vertexComparator, edgeComparator);
			isomorph = inspect.isomorphismExists();
			LOG.trace("graphs {} and {} isomorph? {}", g1, g2, isomorph);
		} catch (ParseException pe) {
			LOG.warn("Both GraphPatterns {} and {} should be parseable.", gp1, gp2, pe);
		}
		return isomorph;
	}

	/**
	 * Convert the given pattern to a JGraphT graph so that it can be used for
	 * isomorphism testing.
	 * 
	 * @param epb The graph pattern to convert into a JGraphT.
	 * @return An JGraphT Graph object.
	 */
	private static Graph<Node, NodeEdge> convertToJGraph(ElementPathBlock epb) {

		org.jgrapht.Graph<Node, NodeEdge> g = new DefaultDirectedGraph<>(NodeEdge.class);

		List<TriplePath> triples = epb.getPattern().getList();
		for (TriplePath t : triples) {

			Node subject = t.getSubject();
			Node predicate = t.getPredicate();
			Node object = t.getObject();
			g.addVertex(subject);
			g.addVertex(object);
			g.addEdge(subject, object, new NodeEdge(predicate));
		}

		return g;
	}

	/**
	 * Specific Edge comparator, because in RDF edges who's Node's are equal, are
	 * still different edges, while vertices who's Node's are equal are the same
	 * Nodes.
	 */
	private static class EdgeComparator implements Comparator<NodeEdge> {

		/**
		 * If both are variables or blank nodes return 0. Otherwise, check if the nodes
		 * are actually equal value wise and return 0, otherwise -1.
		 */
		public int compare(NodeEdge n1, NodeEdge n2) {

			if (n1.getNode().isVariable() && n2.getNode().isVariable())
				return 0;
			else if (n1.getNode().isBlank() && n2.getNode().isBlank())
				return 0;

			return n1.getNode().equals(n2.getNode()) ? 0 : -1;
		}
	}

	/**
	 * Used in the VF2 isomorphism algorithm of JGraphT to compare vertices.
	 * 
	 * @author nouwtb
	 *
	 */
	private static class VertexComparator implements Comparator<Node> {

		/**
		 * If both are variables or both are blank nodes, return 0. If they are actually
		 * equal, return 0, otherwise return -1.
		 */
		public int compare(Node n1, Node n2) {

			if (n1.isVariable() && n2.isVariable())
				return 0;
			else if (n1.isBlank() && n2.isBlank())
				return 0;

			return n1.equals(n2) ? 0 : -1;
		}
	}

	/**
	 * A edge object to capture the node and distinguish from the vertex nodes. This
	 * is necessary, because we want to have the default Object equals() method,
	 * instead of the Node's equals() method. Object's equals() method just checks
	 * using the {@code ==} operator, instead of actually looking at the label of
	 * the node. For edges of the graph, this is the correct behaviour, but for
	 * vertices in the graph this is not.
	 * 
	 * @author nouwtb
	 *
	 */
	private static class NodeEdge extends DefaultEdge {

		private static final long serialVersionUID = 1L;

		/**
		 * The node that represents this edge.
		 */
		private Node n;

		/**
		 * Create a new edge node
		 * 
		 * @param aNode The node this edge node holds.
		 */
		public NodeEdge(Node aNode) {
			this.n = aNode;
		}

		/**
		 * @return the node of the edge node.
		 */
		public Node getNode() {
			return this.n;
		}

		/**
		 * Return a human readable string.
		 */
		public String toString() {
			return this.n.toString();
		}
	}
}
