package eu.knowledge.engine.reasoner.api;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.sse.SSE;

public class TriplePattern {
	private final Node subject;
	private final Node predicate;
	private final Node object;
	private int hashCodeValue;

	public TriplePattern(Node subject, Node predicate, Node object) {
		// TODO I assume a variable name is used only once
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public TriplePattern(String string) {
		this(new PrefixMappingZero(), string);
	}

	public TriplePattern(PrefixMapping prefixes, String aPattern) {
		Triple t = SSE.parseTriple("(" + aPattern + ")", prefixes);
		this.subject = t.getSubject();
		this.predicate = t.getPredicate();
		this.object = t.getObject();

		this.hashCodeValue = this.calcHashCode();
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
	 * between the two triples. This map is used to translate a binding with the
	 * variable names from one triple the variable names of the other.
	 *
	 * The mapping is null if the triple patterns conflict, empty if nothing needs
	 * to be mapped to translate a bindingset from one to the other and non-empty if
	 * something needs to happen to translate one thing to the other.
	 *
	 * @param other
	 * @return
	 */
	public Map<TripleNode, TripleNode> findMatches(TriplePattern other) {
		Map<TripleNode, TripleNode> substitutionMap = new HashMap<>();

		if (this.getSubject() instanceof Var || other.getSubject() instanceof Var) {
			substitutionMap.put(new TripleNode(this, this.getSubject(), 0),
					new TripleNode(other, other.getSubject(), 0));
		} else {
			if (!this.getSubject().equals(other.getSubject())) {
				return null;
			}
		}

		if (this.getPredicate() instanceof Var || other.getPredicate() instanceof Var) {
			substitutionMap.put(new TripleNode(this, this.getPredicate(), 1),
					new TripleNode(other, other.getPredicate(), 1));
		} else {
			if (!this.getPredicate().equals(other.getPredicate())) {
				return null;
			}
		}

		if (this.getObject() instanceof Var || other.getObject() instanceof Var) {
			substitutionMap.put(new TripleNode(this, this.getObject(), 2), new TripleNode(other, other.getObject(), 2));
		} else {
			if (!this.getObject().equals(other.getObject())) {
				return null;
			}
		}

		return substitutionMap;
	}

	@Override
	public String toString() {
		return trunc(subject) + " " + trunc(predicate) + " " + trunc(object);
	}

	public static String trunc(Node n) {

		if (n.isURI()) {
			URI uri = URI.create(n.getURI());

			if (uri.getFragment() != null) {
				return uri.getFragment();
			}
			var path = uri.getPath();
			if (path != null)
				return path.substring(path.lastIndexOf('/') + 1);
		} else if (n.isLiteral()) {
			return n.getLiteralLexicalForm();
		}
		return n.toString();

	}

	public Set<Var> getVariables() {

		Set<Var> vars = new HashSet<>();
		if (this.getSubject() instanceof Var) {
			vars.add((Var) this.getSubject());
		}
		if (this.getPredicate() instanceof Var) {
			vars.add((Var) this.getPredicate());
		}
		if (this.getObject() instanceof Var) {
			vars.add((Var) this.getObject());
		}

		return vars;
	}

	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
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
