package eu.knowledge.engine.smartconnector.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.lang.arq.javacc.ParseException;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.api.SmartConnectorConfig;

public class Util {
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	static {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		CacheManager cacheManager = cachingProvider.getCacheManager();

		long cacheSize = Long.parseLong(
				ConfigProvider.getConfig().getConfigValue(SmartConnectorConfig.CONF_KEY_KE_CACHE_NODE_SIZE).getValue());

		long timeInMinutes = Long.parseLong(ConfigProvider.getConfig()
				.getConfigValue(SmartConnectorConfig.CONF_KEY_KE_CACHE_NODE_EXPIRYMINUTES).getValue());

		CacheConfigurationBuilder<String, Node> ehConfig = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(String.class, Node.class, ResourcePoolsBuilder.heap(cacheSize))
				.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(java.time.Duration.ofMinutes(timeInMinutes)));

		Configuration<String, Node> config = Eh107Configuration.fromEhcacheCacheConfiguration(ehConfig);
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

				m.add(m.asStatement(Triple.create(newNodes[0], newNodes[1], newNodes[2])));
			}
		}
		return m;
	}

	public static Set<TriplePattern> translateGraphPatternTo(GraphPattern pattern) {

		TriplePattern tp;
		TriplePath triplePath;
		String triple;
		ElementPathBlock epb = pattern.getGraphPattern();
		Iterator<TriplePath> iter = epb.patternElts();

		Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();

		while (iter.hasNext()) {

			triplePath = iter.next();

			triple = FmtUtils.stringForTriple(triplePath.asTriple(), new PrefixMappingZero());

			tp = new TriplePattern(triple);
			triplePatterns.add(tp);
		}

		return triplePatterns;
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
