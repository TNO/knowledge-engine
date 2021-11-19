package eu.knowledge.engine.reasoner.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;

import eu.knowledge.engine.reasoner.api.TriplePattern;

public class TriplePattern {
	private final Node subject;
	private final Node predicate;
	private final Node object;

	public TriplePattern(Node subject, Node predicate, Node object) {
		// TODO I assume a variable name is used only once
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public TriplePattern(String string) {
		Triple t = SSE.parseTriple("(" + string + ")", new PrefixMappingZero());
		this.subject = t.getSubject();
		this.predicate = t.getPredicate();
		this.object = t.getObject();
	}

	public Node getSubject() {
		return subject;
	}

	public Node getPredicate() {
		return predicate;
	}

	public Node getObject() {
		return object;
	}

	/**
	 * Checks if two Triples match, and when they do, return a map of how values map
	 * between the two triples. This map is used to translatae a binding with the
	 * variable names from one triple the variable names of the other.
	 *
	 * The mapping is null if the triple patterns conflict, empty if nothing needs
	 * to be mapped to translate a bindingset from one to the other and non-empty if
	 * something needs to happen to translate one thing to the other.
	 *
	 * @param other
	 * @return
	 */
	public Map<Node, Node> findMatches(TriplePattern other) {
		Map<Node, Node> substitutionMap = new HashMap<>();

		if (this.getSubject() instanceof Node_Variable || other.getSubject() instanceof Node_Variable) {
			substitutionMap.put(this.getSubject(), other.getSubject());
		} else {
			if (!this.getSubject().equals(other.getSubject())) {
				return null;
			}
		}

		if (this.getPredicate() instanceof Node_Variable || other.getPredicate() instanceof Node_Variable) {
			substitutionMap.put(this.getPredicate(), other.getPredicate());
		} else {
			if (!this.getPredicate().equals(other.getPredicate())) {
				return null;
			}
		}

		if (this.getObject() instanceof Node_Variable || other.getObject() instanceof Node_Variable) {
			substitutionMap.put(this.getObject(), other.getObject());
		} else {
			if (!this.getObject().equals(other.getObject())) {
				return null;
			}
		}

		return substitutionMap;
	}

	@Override
	public String toString() {
		return subject + " " + predicate + " " + object;
	}

	public Set<Node_Variable> getVariables() {

		Set<Node_Variable> vars = new HashSet<>();
		if (this.getSubject() instanceof Node_Variable) {
			vars.add((Node_Variable) this.getSubject());
		}
		if (this.getPredicate() instanceof Node_Variable) {
			vars.add((Node_Variable) this.getPredicate());
		}
		if (this.getObject() instanceof Node_Variable) {
			vars.add((Node_Variable) this.getObject());
		}

		return vars;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TriplePattern)) {
			return false;
		}
		TriplePattern other = (TriplePattern) obj;
		if (object == null) {
			if (other.object != null) {
				return false;
			}
		} else if (!object.equals(other.object)) {
			return false;
		}
		if (predicate == null) {
			if (other.predicate != null) {
				return false;
			}
		} else if (!predicate.equals(other.predicate)) {
			return false;
		}
		if (subject == null) {
			if (other.subject != null) {
				return false;
			}
		} else if (!subject.equals(other.subject)) {
			return false;
		}
		return true;
	}

}
