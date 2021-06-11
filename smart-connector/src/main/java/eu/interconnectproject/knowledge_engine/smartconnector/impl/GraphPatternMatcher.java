package eu.interconnectproject.knowledge_engine.smartconnector.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.graph.DirectedMultigraph;
import edu.ucla.sspace.graph.DirectedTypedEdge;
import edu.ucla.sspace.graph.SimpleDirectedTypedEdge;
import edu.ucla.sspace.graph.isomorphism.TypedVF2IsomorphismTester;
import eu.interconnectproject.knowledge_engine.smartconnector.api.Binding;
import eu.interconnectproject.knowledge_engine.smartconnector.api.BindingSet;
import eu.interconnectproject.knowledge_engine.smartconnector.api.GraphPattern;

public class GraphPatternMatcher {

	private static final Logger LOG = LoggerFactory.getLogger(GraphPatternMatcher.class);

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

		// first create a mapping from varible name to variable name
		Map<String, String> varMapping = new HashMap<String, String>();

		// convert graphs
		GraphInfo from = new GraphInfo(fromVarNameGP);
		GraphInfo to = new GraphInfo(toVarNameGP);

		TypedVF2IsomorphismTester tester = new TypedVF2IsomorphismTester();
		Map<Integer, Integer> mapping = tester.findIsomorphism(from.getGraph(), to.getGraph());

		if (!mapping.isEmpty()) {

			// store the mapping between variable node vertices.
			for (Integer fromVertexIdx : from.getGraph().vertices()) {

				Node fromNode = from.getIntToNodeMap().get(fromVertexIdx);
				Integer toVertexIdx = mapping.get(fromVertexIdx);
				Node toNode = to.getIntToNodeMap().get(toVertexIdx);

				if (fromNode.isVariable() && toNode.isVariable()) {
					varMapping.put(fromNode.getName(), toNode.getName());
				}

				LOG.trace("Node: {} - {}", fromNode, toNode);
			}

			// now store the mapping between variable node edges. Look at JGraphTs
			// implementation of the getEdgeCorrespondence method.
			for (DirectedTypedEdge<NodeEdge> fromEdge : from.getGraph().edges()) {

				Integer fromEdgeSource = fromEdge.from();
				Integer fromEdgeTarget = fromEdge.to();
				Map<Integer, Node> fromIntToNodeMap = from.getIntToNodeMap();

				if (fromEdge.edgeType().getNode().isVariable()) {

					Set<Node> fromEdgeNodes = getEdgeNodesBetween(fromVarNameGP, fromIntToNodeMap.get(fromEdgeSource),
							fromIntToNodeMap.get(fromEdgeTarget));

					LOG.trace("Found {} number of nodes for from edge {} ({}).", fromEdgeNodes.size(), fromEdgeNodes,
							fromEdge);

					Node fromEdgeNode = null;
					for (Node node : fromEdgeNodes) {
						if (node.isVariable() && fromEdgeNode == null) {

							// check if the current candidate is not already mapped to another edge.
							if (!varMapping.containsKey(node.getName())) {
								fromEdgeNode = node;
							} else {
								LOG.trace("Predicate variable {} is already mapped, so we skip this one.",
										node.getName());
							}
						}
					}

					Integer toEdgeSource = mapping.get(fromEdgeSource);
					Integer toEdgeTarget = mapping.get(fromEdgeTarget);

					/*
					 * Unfortunately, S-Space keeps a *static* collection of edgetypes for
					 * performance reasons and this means we cannot retrieve the exact Node from the
					 * toGraph (because it was replaced by the same variable node of the from
					 * graph). So, how do we get to the edges between the two nodes? Probably using
					 * the Graph Patterns themselves.
					 */
//					Set<DirectedTypedEdge<NodeEdge>> toEdgeSet = to.getGraph().getEdges(toEdgeSource, toEdgeTarget,
//							edgeSet);

					Map<Integer, Node> toIntToNodeMap = to.getIntToNodeMap();
					Set<Node> toEdgeNodes = getEdgeNodesBetween(toVarNameGP, toIntToNodeMap.get(toEdgeSource),
							toIntToNodeMap.get(toEdgeTarget));

					LOG.trace("Found {} number of nodes for to edge {}.", toEdgeNodes.size(), toEdgeNodes);

					assert !toEdgeNodes.isEmpty() : "If the source " + fromEdgeSource + ", " + toEdgeSource
							+ " and target " + fromEdgeSource + ", " + toEdgeSource
							+ " of an edge are isomorph, the mapped source and target should have at least a single edge between them.";

					Node toEdge = null;
					for (Node node : toEdgeNodes) {
						if (node.isVariable() && toEdge == null) {

							// check if the current candidate is not already mapped to another edge.
							if (!varMapping.containsValue(node.getName())) {
								toEdge = node;
							} else {
								LOG.trace("Predicate variable {} is already mapped, so we skip this one.",
										node.getName());
							}
						}
					}

					if (toEdge != null && fromEdgeNode != null) {
						varMapping.put(fromEdgeNode.getName(), toEdge.getName());

						LOG.trace("Edge: {} - {}", fromEdgeNode, toEdge);
					} else {
						assert false : "The two graph patterns " + fromVarNameGP + " and " + toVarNameGP
								+ " match, so the variable edge " + fromEdge
								+ " should be mappable to another variable edge.";
					}
				}
			}

			LOG.trace("Isomorphism {} gave rise to variable mapping: {}", mapping, varMapping);

		} else {
			LOG.trace("No isomorphisms found.");
		}

		// now transform the from bindingset to the to bindingset using the variable
		// mapping.
		BindingSet toBindingSet = new BindingSet();

		Binding toBinding;
		for (Binding fromBinding : fromBindingSet) {
			toBinding = new Binding();
			for (Map.Entry<String, String> twoVars : varMapping.entrySet()) {
				if (fromBinding.containsKey(twoVars.getKey())) {
					toBinding.put(twoVars.getValue(), fromBinding.get(twoVars.getKey()));
				} else {
					// the binding could be partial, so not every mapped variable should be
					// available in the binding.
				}
			}
			toBindingSet.add(toBinding);
		}

		LOG.trace("Transformed bindingset size {} into size {} or {} into {}.", fromBindingSet.size(), toBindingSet.size(),
				fromBindingSet, toBindingSet);

		return toBindingSet;

	}

	private static Set<Node> getEdgeNodesBetween(GraphPattern gp, Node source, Node target) {
		ElementPathBlock epb;
		Set<Node> nodeSet = new HashSet<>();
		epb = gp.getGraphPattern();
		List<TriplePath> triples = epb.getPattern().getList();
		for (TriplePath t : triples) {

			Node subject = t.getSubject();
			Node predicate = t.getPredicate();
			Node object = t.getObject();

			if (subject.equals(source) && object.equals(target)) {
				nodeSet.add(predicate);
			}

		}
		return nodeSet;
	}

	private static class NodeEdge {

		private Node n;

		public NodeEdge(Node n) {
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
				return 31; // TODO this is not good performance wise
			} else {
				return this.n.hashCode();
			}
		}

		@Override
		public String toString() {
			if (this.n.isVariable()) {

				return n.getName();
			} else {
				return this.n.toString();
			}
		}

		public Node getNode() {
			return n;
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

		GraphInfo g1 = new GraphInfo(gp1);
		GraphInfo g2 = new GraphInfo(gp2);
		LOG.trace("are isomorph? g1: {}, g2: {}", g1, g2);

		// first retrieve the isomorphisms
		TypedVF2IsomorphismTester tester = new TypedVF2IsomorphismTester();
		Map<Integer, Integer> mapping = tester.findIsomorphism(g1.getGraph(), g2.getGraph());

		// because nodes are translated into integers, the algorithm cannot distinguish
		// between different concrete nodes. This means that every concrete node is
		// similar to any other concrete node, but this is of course not the case.
		for (Map.Entry<Integer, Integer> entry : mapping.entrySet()) {

			var g1map = g1.getIntToNodeMap();
			var g2map = g2.getIntToNodeMap();

			Node n1 = g1map.get(entry.getKey());
			Node n2 = g2map.get(entry.getValue());

			// if we find two isomorph concrete nodes that are not equal, the whole mapping
			// is invalid.
			if (n1.isConcrete() && n2.isConcrete() && !n1.equals(n2)) {
				return new HashMap<Integer, Integer>();
			}
		}

		return mapping;
	}

	public static boolean areIsomorphic(GraphPattern gp1, GraphPattern gp2) throws ParseException {
		return !getIsomorphisms(gp1, gp2).isEmpty();
	}

	private static class GraphInfo {

		private DirectedMultigraph<NodeEdge> graph;
		private Map<Integer, Node> intToNodeMap;

		public GraphInfo(GraphPattern gp) {
			ElementPathBlock epb = gp.getGraphPattern();

			// initialize and convert fromGraph
			this.graph = new DirectedMultigraph<>();

			this.intToNodeMap = new HashMap<>();
			Map<Node, Integer> nodeToIntMap = new HashMap<>();
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
		}

		public DirectedMultigraph<NodeEdge> getGraph() {
			return graph;
		}

		public Map<Integer, Node> getIntToNodeMap() {
			return intToNodeMap;
		}
	}

}
