package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * A binding where the keys are triple/variable pairs.
 * 
 * @author nouwtb
 *
 */
public class TripleVarBinding {

	private Map<TripleNode, Node> tripleVarMapping;

	/**
	 * Optimize the getVarValue method.
	 */
	private Map<Var, TripleNode> variableTripleVarMapping;

	public TripleVarBinding() {
		tripleVarMapping = new HashMap<>(6, 1.0f);
		variableTripleVarMapping = new HashMap<>(4, 1.0f);
	}

	public TripleVarBinding(Set<TriplePattern> aGraphPattern, Binding aBinding) {
		this();
		TripleNode tripleVar;
		for (TriplePattern tp : aGraphPattern) {

			if (tp.getSubject().isVariable() && aBinding.containsKey(tp.getSubject())) {
				tripleVar = new TripleNode(tp, tp.getSubject(), 0);
				tripleVarMapping.put(tripleVar, aBinding.get(tp.getSubject()));
				variableTripleVarMapping.put((Var) tp.getSubject(), tripleVar);
			}
			if (tp.getPredicate().isVariable() && aBinding.containsKey(tp.getPredicate())) {
				tripleVar = new TripleNode(tp, tp.getPredicate(), 1);
				tripleVarMapping.put(tripleVar, aBinding.get(tp.getPredicate()));
				variableTripleVarMapping.put((Var) tp.getPredicate(), tripleVar);
			}
			if (tp.getObject().isVariable() && aBinding.containsKey(tp.getObject())) {
				tripleVar = new TripleNode(tp, tp.getObject(), 2);
				tripleVarMapping.put(tripleVar, aBinding.get(tp.getObject()));
				variableTripleVarMapping.put((Var) tp.getObject(), tripleVar);
			}
		}
	}

	public TripleVarBinding(TripleVarBinding b) {
		this();
		for (Map.Entry<TripleNode, Node> entry : b.tripleVarMapping.entrySet()) {
			this.put(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<Var, TripleNode> entry : b.variableTripleVarMapping.entrySet()) {
			this.variableTripleVarMapping.put(entry.getKey(), entry.getValue());
		}
	}

	public Set<Map.Entry<TripleNode, Node>> entrySet() {
		return tripleVarMapping.entrySet();
	}

	public Set<TripleNode> getTripleVars() {
		return tripleVarMapping.keySet();
	}

	public void put(TripleNode aTripleVar, Node aLiteral) {
		assert aTripleVar.node.isVariable();

		if (!aLiteral.isConcrete())
			throw new IllegalArgumentException(
					"Binding values should be concrete nodes (either RDF literals or URIs) and not '"
							+ aLiteral.toString() + "'");

		tripleVarMapping.put(aTripleVar, aLiteral);
		variableTripleVarMapping.put((Var) aTripleVar.node, aTripleVar);
	}

	public void put(TripleNode aTripleVar, String aLiteral) {
		this.put(aTripleVar, (Node) SSE.parseNode(aLiteral));
	}

	/**
	 * Convert the TripleVarBinding to the Binding. I.e. collapse the TripleVars
	 * with the same Vars into Vars. Note that if a particular triplevar is
	 * undefined, but another triple with the same var is defined, it will show up
	 * in the resulting binding.
	 * 
	 * @return
	 */
	public Binding toBinding() {

		Binding b = new Binding();
		for (Map.Entry<TripleNode, Node> entry : this.tripleVarMapping.entrySet()) {
			assert entry.getKey().node.isVariable();
			b.put((Var) entry.getKey().node, entry.getValue());
		}
		return b;
	}

	/**
	 * True if two Bindings have a different value for at least one variable. Note
	 * that it looks not at variable instances.
	 */
	public boolean isConflicting(TripleVarBinding tvb) {

		Node l;
		for (Map.Entry<TripleNode, Node> e : this.tripleVarMapping.entrySet()) {
			assert e.getKey().node.isVariable();
			l = tvb.getVarValue((Var) e.getKey().node);

			if (l != null && !e.getValue().sameValueAs(l)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * We assume all occurrences of a var have the same literal, we just return the
	 * first one found.
	 * 
	 * @param variable
	 * @return
	 */
	public Node getVarValue(Var variable) {
		TripleNode tripleVar = this.variableTripleVarMapping.get(variable);
		return this.get(tripleVar);
	}

	public Node get(TripleNode key) {
		return this.tripleVarMapping.get(key);
	}

	public boolean containsKey(TripleNode key) {
		return this.tripleVarMapping.containsKey(key);
	}

	public Set<TripleNode> getTripleNodes(TriplePattern aTriplePattern) {
		Set<TripleNode> nodes = new HashSet<TripleNode>();
		for (TripleNode tv : this.tripleVarMapping.keySet()) {
			if (tv.tp.equals(aTriplePattern)) {
				nodes.add(tv);
			}
		}
		return nodes;
	}

	public boolean containsVar(Var aVar) {
		assert aVar instanceof Var;

		for (TripleNode tNode : this.tripleVarMapping.keySet()) {
			if (tNode.node.sameValueAs(aVar)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tripleVarMapping == null) ? 0 : tripleVarMapping.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TripleVarBinding)) {
			return false;
		}
		TripleVarBinding other = (TripleVarBinding) obj;
		if (tripleVarMapping == null) {
			if (other.tripleVarMapping != null) {
				return false;
			}
		} else if (!tripleVarMapping.equals(other.tripleVarMapping)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		String prefix = "";
		sb.append("{");
		for (Map.Entry<TripleNode, Node> entry : this.tripleVarMapping.entrySet()) {
			sb.append(prefix).append(entry.getKey().node).append("=")
					.append(FmtUtils.stringForNode(entry.getValue(), new PrefixMappingZero()));
			prefix = ",";

		}
		sb.append("}");
		return sb.toString();
	}

	public Set<TripleNode> keySet() {
		return this.tripleVarMapping.keySet();
	}

	/**
	 * We assume these two triplevarbindings do not conflict (responsibility of the
	 * caller to check!) and return the merged triplevarbinding. Duplicates are
	 * automatically removed because triple var bindings are sets.
	 * 
	 * @param otherB
	 * @return
	 */
	public TripleVarBinding merge(TripleVarBinding otherB) {

		assert !this.isConflicting(otherB);
		TripleVarBinding b = new TripleVarBinding();
		b.putAll(this.tripleVarMapping);
		b.putAll(otherB.tripleVarMapping);

		return b;
	}

	private void putAll(Map<TripleNode, Node> aTripleVarMapping) {

		for (TripleNode tv : aTripleVarMapping.keySet()) {
			assert tv.node.isVariable();
			this.variableTripleVarMapping.put((Var) tv.node, tv);
		}
		this.tripleVarMapping.putAll(aTripleVarMapping);
	}

	public boolean isEmpty() {
		return this.tripleVarMapping.isEmpty();
	}

	public boolean containsTriplePattern(TriplePattern value) {
		for (TripleNode tv : this.tripleVarMapping.keySet()) {
			if (tv.tp.equals(value)) {
				return true;
			}
		}
		return false;
	}

}
