package interconnect.ke.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.sse.SSE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.sc.SmartConnectorBuilder;

public class TestUtils {

	/**
	 * The log facility of this class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

	public static final GraphPattern SAREF_MEASUREMENT_PATTERN = new GraphPattern(
			"?m <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Measurement> . ?m <https://saref.etsi.org/core/hasValue> ?v");

	private static final String KE_PREFIX = "https://www.interconnectproject.eu/";

	public static final SmartConnector getSmartConnector(final String aName) {
		return SmartConnectorBuilder.newSmartConnector(new KnowledgeBase() {

			@Override
			public URI getKnowledgeBaseId() {
				URI uri = null;
				try {
					uri = new URI(KE_PREFIX + aName);
				} catch (URISyntaxException e) {
					LOG.error("Could not parse the uri.", e);
				}
				return uri;
			}

			@Override
			public String getKnowledgeBaseDescription() {
				return null;
			}

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
			}

			@Override
			public void smartConnectorConnectionLost(SmartConnector aSC) {
			}

			@Override
			public void smartConnectorConnectionRestored(SmartConnector aSC) {
			}

			@Override
			public String getKnowledgeBaseName() {
				return null;
			}

			@Override
			public void smartConnectorStopped(SmartConnector aSC) {
				// TODO Auto-generated method stub

			}
		}).create();
	}

	public static final Set<Binding> getSingleBinding(String name, String value) {
		Set<Binding> bindings = new HashSet<>();
		Binding b = new Binding();
		b.put(name, value);
		bindings.add(b);
		return bindings;
	}

	public static final BindingSet getSingleBinding(String name1, String value1, String name2, String value2) {
		BindingSet bindings = new BindingSet();
		Binding b = new Binding();
		b.put(name1, value1);
		b.put(name2, value2);
		bindings.add(b);
		return bindings;
	}

	public static final int getIntegerValueFromString(String value) {
		// TODO See issue #3
		Pattern p = Pattern.compile("^\"(\\w+)\"\\^\\^\\<http://www.w3.org/2001/XMLSchema#integer\\>$");
		Matcher m = p.matcher(value);
		return Integer.parseInt(m.group(1));
	}

	public static final String getStringValueFromInteger(int value) {
		return "\"" + value + "\"^^<http://www.w3.org/2001/XMLSchema#integer>";
	}

	public static final String getWithPrefix(String value) {
		return KE_PREFIX + value;
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

//							newN = NodeFactoryExtra.parseNode(repr);
						} else {
							LOG.error("The variable {} in the Knowledge should be bound.", n.getName());
						}
					}
					newNodes[i] = newN;
				}

				m.add(m.asStatement(new Triple(newNodes[0], newNodes[1], newNodes[2])));
			}
		}
		return m;
	}

}
