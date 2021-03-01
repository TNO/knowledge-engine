package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.graph.DirectedMultigraph;
import edu.ucla.sspace.graph.SimpleDirectedTypedEdge;
import edu.ucla.sspace.graph.isomorphism.TypedVF2IsomorphismTester;
import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
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
	public static VF2GraphIsomorphismInspector<Node, NodeEdgeOld> getIsomorphisms(ElementPathBlock epb1,
			ElementPathBlock epb2) {

		Graph<Node, NodeEdgeOld> g1 = convertToJGraph(epb1);
		Graph<Node, NodeEdgeOld> g2 = convertToJGraph(epb2);
		VF2GraphIsomorphismInspector<Node, NodeEdgeOld> inspect = new VF2GraphIsomorphismInspector<Node, NodeEdgeOld>(
				g1, g2, vertexComparator, edgeComparator);

		return inspect;
	}

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
	public static boolean checkIsomorph(ElementPathBlock epb1, ElementPathBlock epb2) {

		VF2GraphIsomorphismInspector<Node, NodeEdgeOld> iso = getIsomorphisms(epb1, epb2);

		Iterator<GraphMapping<Node, NodeEdgeOld>> mappings = iso.getMappings();

		boolean isomorph = mappings.hasNext();
		LOG.trace("graphs {} and {} isomorph? {}", epb1, epb2, isomorph);
		return isomorph;
	}

	/**
	 * Convert the given pattern to a JGraphT graph so that it can be used for
	 * isomorphism testing.
	 * 
	 * @param epb The graph pattern to convert into a JGraphT.
	 * @return An JGraphT Graph object.
	 */
	public static Graph<Node, NodeEdgeOld> convertToJGraph(ElementPathBlock epb) {

		// unfortunately, we cannot use a #DirectedPseudograph graph (which allows
		// multiple edges between vertices), because it is not supported by the JGraphT
		// isomorphism algorithm. We could consider using NetworkX together with Jython.
		org.jgrapht.Graph<Node, NodeEdgeOld> g = new DefaultDirectedGraph<>(NodeEdgeOld.class);

		List<TriplePath> triples = epb.getPattern().getList();
		for (TriplePath t : triples) {

			Node subject = t.getSubject();
			Node predicate = t.getPredicate();
			Node object = t.getObject();

			boolean success = g.addVertex(subject);
			success = g.addVertex(object);
			NodeEdgeOld e = new NodeEdgeOld(predicate);
			success = g.addEdge(subject, object, e);
			if (!success)
				LOG.warn("Adding edge {} between {} and {} to the graph should succeed.", e, subject, object);
		}

		return g;
	}

	/**
	 * Transform all the variable names in a bindingset from one knowledge
	 * interaction to another knowledge interaction.
	 * 
	 * @param fromVarNameKI
	 * @param toVarNameKI
	 * @param fromBindingSet
	 * @return A bindingset that uses the variable names of toVarNameKI instead of
	 *         fromVarNameKI.
	 */
	public static BindingSet transformBindingSet(GraphPattern fromVarNameGP, GraphPattern toVarNameGP,
			BindingSet fromBindingSet) {

		LOG.trace("Looking for isomorphisms between: {} and {}", fromVarNameGP, toVarNameGP);
		ElementPathBlock fromEpb;
		ElementPathBlock toEpb;

		// first create a mapping from varible name to variable name
		Map<String, String> varMapping = new HashMap<String, String>();
		try {

			fromEpb = fromVarNameGP.getGraphPattern();
			toEpb = toVarNameGP.getGraphPattern();

			// convert graphs
			GraphInfo from = new GraphInfo(fromVarNameGP);
			GraphInfo to = new GraphInfo(toVarNameGP);

			TypedVF2IsomorphismTester tester = new TypedVF2IsomorphismTester();
			Map<Integer, Integer> mapping = tester.findIsomorphism(from.getGraph(), to.getGraph());

			if (!mapping.isEmpty()) {

				GraphMapping<Node, NodeEdgeOld> mapping = iter.next();

				for (Node vertex : fromGraph.vertexSet()) {
					Node n = mapping.getVertexCorrespondence(vertex, true);

					if (vertex.isVariable()) {
						varMapping.put(vertex.getName(), n.getName());
					}

					LOG.trace("Node: {} - {}", vertex, n);
				}

				for (NodeEdgeOld edge : fromGraph.edgeSet()) {
					NodeEdgeOld e = mapping.getEdgeCorrespondence(edge, true);

					if (e.getNode().isVariable()) {
						varMapping.put(edge.getNode().getName(), e.getNode().getName());
					}

					LOG.trace("Edge: {} - {}", edge, e);
				}

				LOG.trace("mapping: {}", mapping);

			}

			LOG.debug("Has more than one mapping? {}", iter.hasNext());
			LOG.debug("Found the following variable mapping: {}", varMapping);

		} catch (ParseException e) {
			LOG.error("Parsing graph patterns should succeed.", e);
		}
		LOG.debug("Variable mapping: {}", varMapping);

		// now transform the from bindingset to the to bindingset using the variable
		// mapping.
		BindingSet toBindingSet = new BindingSet();

		Binding to;
		for (Binding from : fromBindingSet) {
			to = new Binding();
			for (Map.Entry<String, String> twoVars : varMapping.entrySet()) {
				if (from.containsKey(twoVars.getKey())) {
					to.put(twoVars.getValue(), from.get(twoVars.getKey()));
				}
			}
			toBindingSet.add(to);
		}

		LOG.info("Transformed size {} into size {} or {} into {}.", fromBindingSet.size(), toBindingSet.size(),
				fromBindingSet, toBindingSet);

		return toBindingSet;

	}

	/**
	 * Specific Edge comparator, because in RDF edges who's Node's are equal, are
	 * still different edges, while vertices who's Node's are equal are the same
	 * Nodes.
	 */
	private static class EdgeComparator implements Comparator<NodeEdgeOld> {

		/**
		 * If both are variables or blank nodes return 0. Otherwise, check if the nodes
		 * are actually equal value wise and return 0, otherwise -1.
		 */
		public int compare(NodeEdgeOld n1, NodeEdgeOld n2) {

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
	 * An edge object to capture the node and distinguish from the vertex nodes.
	 * This is necessary, because we want to have the default Object equals()
	 * method, instead of the Node's equals() method. Object's equals() method just
	 * checks using the {@code ==} operator, instead of actually looking at the
	 * label of the node. For edges of the graph, this is the correct behaviour, but
	 * for vertices in the graph this is not.
	 * 
	 * @author nouwtb
	 *
	 */
	private static class NodeEdgeOld extends DefaultEdge {

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
		public NodeEdgeOld(Node aNode) {
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

	private static class NodeEdge {

		private Node n;

		public NodeEdge(Node n) {
			super();
			this.n = n;
		}

		@Override
		public boolean equals(Object obj) {

			NodeEdge otherN = (NodeEdge) obj;

			if ((otherN.n.isVariable() && this.n.isVariable())) {
				return true;
			} else {
				return otherN.n.equals(this.n);
			}
		}

		@Override
		public int hashCode() {
			if (this.n.isVariable()) {
				return 31;
			} else {
				return this.n.hashCode();
			}
		}

		@Override
		public String toString() {
			if (this.n.isVariable()) {

				return "?var";
			} else {
				return this.n.toString();
			}
		}

	}

	private static DirectedMultigraph<NodeEdge> convertToSSpace(ElementPathBlock epb) {

		DirectedMultigraph<NodeEdge> g = new DirectedMultigraph<>();

		Map<Integer, Node> intToNodeMap = new HashMap<>();
		Map<Node, Integer> nodeToIntMap = new HashMap<>();

		List<TriplePath> triples = epb.getPattern().getList();

		int vertexCounter = 1;

		for (TriplePath t : triples) {

			Node subject = t.getSubject();
			Node predicate = t.getPredicate();
			Node object = t.getObject();

			boolean success;
			if (!intToNodeMap.containsValue(subject)) {
				intToNodeMap.put(vertexCounter, subject);
				nodeToIntMap.put(subject, vertexCounter);
				success = g.add(vertexCounter);
			}

			vertexCounter++;
			if (!intToNodeMap.containsValue(object)) {
				intToNodeMap.put(vertexCounter, object);
				nodeToIntMap.put(object, vertexCounter);
				success = g.add(vertexCounter);
			}

			SimpleDirectedTypedEdge<NodeEdge> e = new SimpleDirectedTypedEdge<NodeEdge>(new NodeEdge(predicate),
					nodeToIntMap.get(subject), nodeToIntMap.get(object));

			success = g.add(e);

			if (!success)
				LOG.warn("Adding edge with type {} between {} and {} to the graph should succeed.", e, subject, object);

			vertexCounter++;
		}

		return g;
	}

	public static Map<Integer, Integer> getIsomorphisms(GraphPattern gp1, GraphPattern gp2) throws ParseException {

		DirectedMultigraph<NodeEdge> g1 = convertToSSpace(gp1.getGraphPattern());
		DirectedMultigraph<NodeEdge> g2 = convertToSSpace(gp2.getGraphPattern());
		LOG.trace("are isomorph? g1: {}, g2: {}", g1, g2);

		TypedVF2IsomorphismTester tester = new TypedVF2IsomorphismTester();
		return tester.findIsomorphism(g1, g2);
	}

	public static boolean areIsomorphic(GraphPattern gp1, GraphPattern gp2) throws ParseException {

		DirectedMultigraph<NodeEdge> g1 = convertToSSpace(gp1.getGraphPattern());
		DirectedMultigraph<NodeEdge> g2 = convertToSSpace(gp2.getGraphPattern());
		LOG.trace("are isomorph? g1: {}, g2: {}", g1, g2);

		TypedVF2IsomorphismTester tester = new TypedVF2IsomorphismTester();
		return tester.areIsomorphic(g1, g2);
	}

	private static class GraphInfo {

		private DirectedMultigraph<NodeEdge> graph;
		private Map<Integer, Node> intToNodeMap;
		private Map<Node, Integer> nodeToIntMap;

		public GraphInfo(GraphPattern gp) {

			ElementPathBlock epb;
			try {
				epb = gp.getGraphPattern();

				// initialize and convert fromGraph
				this.graph = new DirectedMultigraph<>();

				this.intToNodeMap = new HashMap<>();
				this.nodeToIntMap = new HashMap<>();

				List<TriplePath> triples = epb.getPattern().getList();

				int fromVertexCounter = 1;

				for (TriplePath t : triples) {

					Node subject = t.getSubject();
					Node predicate = t.getPredicate();
					Node object = t.getObject();

					boolean success;
					if (!intToNodeMap.containsValue(subject)) {
						intToNodeMap.put(fromVertexCounter, subject);
						nodeToIntMap.put(subject, fromVertexCounter);
						success = this.graph.add(fromVertexCounter);
					}

					fromVertexCounter++;
					if (!intToNodeMap.containsValue(object)) {
						intToNodeMap.put(fromVertexCounter, object);
						nodeToIntMap.put(object, fromVertexCounter);
						success = this.graph.add(fromVertexCounter);
					}

					SimpleDirectedTypedEdge<NodeEdge> e = new SimpleDirectedTypedEdge<NodeEdge>(new NodeEdge(predicate),
							nodeToIntMap.get(subject), nodeToIntMap.get(object));

					success = this.graph.add(e);

					if (!success)
						LOG.warn("Adding edge with type {} between {} and {} to the graph should succeed.", e, subject,
								object);

					fromVertexCounter++;
				}
			} catch (ParseException e1) {
				LOG.error("Graph pattern {} should be parseable.", gp);
			}
		}

		public DirectedMultigraph<NodeEdge> getGraph() {
			return graph;
		}

		public Map<Integer, Node> getIntToNodeMap() {
			return intToNodeMap;
		}

		public Map<Node, Integer> getNodeToIntMap() {
			return nodeToIntMap;
		}

	}

}
