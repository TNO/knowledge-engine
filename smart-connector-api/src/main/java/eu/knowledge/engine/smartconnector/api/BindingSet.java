package eu.knowledge.engine.smartconnector.api;

import java.util.HashSet;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.sse.SSE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of bindings. Note that the multiple bindings in this set have an 'or'
 * relation (instead of an 'and' relation) with each other.
 */
public class BindingSet extends HashSet<Binding> {

	private static final Logger LOG = LoggerFactory.getLogger(BindingSet.class);

	private static final long serialVersionUID = 1L;

	/**
	 * Construct a BindingSet from a {@link ResultSet}.
	 * 
	 * @param rs An Apache Jena result set of a SELECT query.
	 */
	public BindingSet(ResultSet rs) {
		QuerySolution qs = null;
		while (rs.hasNext()) {
			qs = rs.next();
			this.add(new Binding(qs));
		}
	}

	public BindingSet() {
	}

	/**
	 * Write this BindingSet to the standard output.
	 * This is convenient for debugging.
	 */
	public void write() {
		for (Binding b : this) {
			System.out.println(b);
		}

	}
}
