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

	// Precalulated the hash code because hashCode() is used so heavily with Var's
	private final int hashCodeValue;

	public TripleNode(TriplePattern aTriplePattern, Node aNode, int aNodeIdx) {
		assert (0 <= aNodeIdx && aNodeIdx <= 2);
		this.tp = aTriplePattern;
		this.node = aNode;
		this.nodeIdx = aNodeIdx;
		this.hashCodeValue = this.calcHashCode();
	}

	public TripleNode(TriplePattern aTriplePattern, String aNode, int aNodeIdx) {
		this(aTriplePattern, SSE.parseNode(aNode), aNodeIdx);
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		boolean firstTime = true;
		for (Node n : new Node[] { tp.getSubject(), tp.getPredicate(), tp.getObject() }) {
			if (!firstTime) {
				sb.append(" ");
			}
			var truncatedNode = TriplePattern.trunc(n);
			if (this.node.sameValueAs(n)) {
				sb.append("|").append(truncatedNode).append("|");
			} else {
				sb.append(truncatedNode);
			}
			firstTime = false;
		}

		return sb.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + nodeIdx;
		result = prime * result + ((tp == null) ? 0 : tp.hashCode());
		return result;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
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