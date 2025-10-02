package eu.knowledge.engine.smartconnector.api;

import java.util.HashSet;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
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
