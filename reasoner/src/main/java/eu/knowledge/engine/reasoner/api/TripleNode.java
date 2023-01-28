package eu.knowledge.engine.reasoner.api;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.sse.SSE;

public class TripleNode {
	public TriplePattern tp;
	public Node node;

	/**
	 * The index of the node of this triplenode. 0 = subject, 1 = predicate, 2 =
	 * object.
	 */
	public int nodeIdx;

	public TripleNode(TriplePattern aTriplePattern, Node aNode, int aNodeIdx) {
		assert (0 <= aNodeIdx && aNodeIdx <= 2);
		this.tp = aTriplePattern;
		this.node = aNode;
		this.nodeIdx = aNodeIdx;
	}

	public TripleNode(TriplePattern aTriplePattern, String aNode, int aNodeIdx) {
		this(aTriplePattern, SSE.parseNode(aNode), aNodeIdx);
	}

	@Override
	public String toString() {
		return "TripleNode [node=" + node + ", tp=" + tp + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result + nodeIdx;
		result = prime * result + ((tp == null) ? 0 : tp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TripleNode other = (TripleNode) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (nodeIdx != other.nodeIdx)
			return false;
		if (tp == null) {
			if (other.tp != null)
				return false;
		} else if (!tp.equals(other.tp))
			return false;
		return true;
	}

}