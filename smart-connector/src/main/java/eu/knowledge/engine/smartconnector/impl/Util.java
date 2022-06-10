package eu.knowledge.engine.smartconnector.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.sse.SSE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;

public class Util {
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	static {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		CacheManager cacheManager = cachingProvider.getCacheManager();
		MutableConfiguration<String, Node> config = new MutableConfiguration<String, Node>()
				.setTypes(String.class, Node.class).setStoreByValue(false)
				.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
		nodeCache = cacheManager.createCache("nodeCache", config);
	}

	private static Cache<String, Node> nodeCache;

	/**
	 * Convert a KnowledgeIO and a Set of bindings into a RDF model with actual
	 * triples.
	 * 
	 * @param graphPattern The Knowledge to populate to a model.
	 * @return A model where all variables of the kIO are populated with URIs.
	 * @throws ParseException
	 */
	public static Model generateModel(GraphPattern graphPattern, BindingSet variableBindings) throws ParseException {

		LOG.trace("generating model");

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

							if ((newN = nodeCache.get(repr)) == null) {
								newN = SSE.parseNode(repr);
								nodeCache.put(repr, newN);
							}

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

	public static void removeRedundantBindingsAnswer(BindingSet incoming, BindingSet outgoing) {
		if (incoming.isEmpty()) {
			// We should not remove any bindings in this case!
			return;
		}

		var toBeRemoved = new ArrayList<Binding>();
		outgoing.forEach(outgoingBinding -> {
			if (incoming.stream().allMatch(incomingBinding -> !incomingBinding.isSubBindingOf(outgoingBinding))) {
				toBeRemoved.add(outgoingBinding);
			}
		});
		outgoing.removeAll(toBeRemoved);
	}
}
