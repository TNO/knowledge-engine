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
	 * Convert a KnowledgeIO and a Set of bindings into a RDF model with actual
	 * triples.
	 *
	 * @param graphPattern The Knowledge to populate to a model.
	 * @return A model where all variables of the kIO are populated with URIs.
	 * @throws ParseException
	 */
	public static Model generateModel(GraphPattern graphPattern, BindingSet variableBindings) throws ParseException {

		List<TriplePath> tripleList = graphPattern.getGraphPattern().getPattern().getList();

		Model m = ModelFactory.createDefaultModel();

		for (Binding b : variableBindings) {

			for (TriplePath tp : tripleList) {

				Node s = tp.getSubject();
				Node p = tp.getPredicate();
				Node o = tp.getObject();

				Node[] oldNodes = new Node[] { s, p, o };
				Node[] newNodes = new Node[3];
				for (int i = 0; i < oldNodes.length; i++) {
					Node n = oldNodes[i];
					Node newN = n;
					if (n.isVariable()) {

						String repr;
						if (b.containsKey(n.getName())) {
							repr = b.get(n.getName());

							LOG.trace("Parsing: {}", repr);

							newN = SSE.parseNode(repr);

							// newN = NodeFactoryExtra.parseNode(repr);
						} else {
							LOG.error("The variable {} should be bound.", n.getName());
						}
					}
					newNodes[i] = newN;
				}

				m.add(m.asStatement(new Triple(newNodes[0], newNodes[1], newNodes[2])));
			}
		}
		return m;
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
