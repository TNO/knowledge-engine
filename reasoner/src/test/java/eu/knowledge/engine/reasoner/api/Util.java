package eu.knowledge.engine.reasoner.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.sse.SSE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	static String getStringFromInputStream(InputStream is) {
		String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining("\n"));

		return text;
	}

	public static Binding toBinding(String encodedBinding) {

		Binding b = new Binding();
		String[] entries = encodedBinding.split(",");

		int varIdx = 0, valIdx = 1;

		for (String entry : entries) {

			if (!entry.isEmpty()) {
				String[] keyVal = entry.split("=");
				b.put(keyVal[varIdx], keyVal[valIdx]);
			}
		}
		return b;
	}

	public static BindingSet toBindingSet(String encodedBindingSet) {

		BindingSet bs = new BindingSet();
		String[] entries = encodedBindingSet.split("\\|");

		for (String entry : entries) {
			Binding b = toBinding(entry);
			bs.add(b);
		}
		return bs;
	}

	/**
	 * Convert a KnowledgeIO and a Set of bindings into a RDF model with actual
	 * triples.
	 * 
	 * @param graphPattern The Knowledge to populate to a model.
	 * @return A model where all variables of the kIO are populated with URIs.
	 * @throws ParseException
	 */
	public static Model generateModel(TriplePattern tp, BindingSet variableBindings) throws ParseException {

		LOG.trace("generating model");

		Model m = ModelFactory.createDefaultModel();

		for (Binding b : variableBindings) {

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

						if (b.get(n.getName()).isURI()) {
							repr = "<" + b.get(n.getName()).toString() + ">";
						} else {
							repr = b.get(n.getName()).toString(); // "\"" + b.get(n.getName()).toString() + "\"";
						}

						LOG.trace("Parsing: {}", repr);
						newN = SSE.parseNode(repr);
					} else {
						LOG.error("The variable {} in the Knowledge should be bound.", n.getName());
					}
				}
				newNodes[i] = newN;
			}

			m.add(m.asStatement(Triple.create(newNodes[0], newNodes[1], newNodes[2])));
		}
		return m;
	}

	/**
	 * parses every line of the string into a triple pattern and combines them into
	 * a set. Note that if it ends with a period (.) it will be removed.
	 * 
	 * @param gp
	 * @return
	 */
	public static Set<TriplePattern> toGP(String gp) {

		var set = new HashSet<TriplePattern>();
		String[] lines = gp.split("\n");

		for (String line : lines) {

			String trimmed = line.strip();
			if (trimmed.endsWith("."))
				trimmed = trimmed.substring(0, trimmed.length() - 2);

			if (!trimmed.isEmpty()) {
				set.add(new TriplePattern(trimmed));
			}
		}

		return set;
	}

}
