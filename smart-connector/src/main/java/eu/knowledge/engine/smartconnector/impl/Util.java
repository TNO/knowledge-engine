package eu.knowledge.engine.smartconnector.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.GraphPattern;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.AnswerBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactBindingSetHandler;

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

	/**
	 * Returns the knowledge gap of this reasoning node. A knowledge gap is a subset
	 * of this node's antecedent triple patterns that do not match any neighbor that
	 * has no knowledge gaps.
	 * 
	 * Currently, this method does not show how the knowledge gaps influence each
	 * other. Some knowledge gaps might have an {@code or}-relation (namely those
	 * that occur on the same triple) and some might have {@code and}-relations
	 * (i.e. those that do not occur on the same triple). This information is
	 * important if you want to know how to solve the gaps because 2 gaps related by
	 * {@code or} do not both need to be solved, but only one of them. While 2 gaps
	 * related by {@code and} both need to be solved to solve the gap.
	 * 
	 * @return returns all triples that have no matching nodes (and for which there
	 *         are no alternatives). Note that it returns a set of sets. Where every
	 *         set in this set represents a single way to resolve the knowledge gaps
	 *         present in this reasoning graph. So, {@code [[A],[B]]} means either
	 *         triple {@code A} <i><b>OR</b></i> triple {@code B} needs be added to
	 *         solve the gap or both, while {@code [[A,B]]} means that both
	 *         {@code A} <i><b>AND</b></i> {@code B} need to be added to solve the
	 *         gap.
	 */
	public static Set<Set<TriplePattern>> getKnowledgeGaps(ReasoningNode root) {

		Set<Set<TriplePattern>> existingOrGaps = new HashSet<>();

		// TODO do we need to include the parent if we are not backward chaining?
		Map<TriplePattern, Set<ReasoningNode>> nodeCoverage = root
				.findAntecedentCoverage(root.getAntecedentNeighbors());

		// collect triple patterns that have an empty set
		Set<Set<TriplePattern>> collectedOrGaps, someGaps = new HashSet<>();
		for (Entry<TriplePattern, Set<ReasoningNode>> entry : nodeCoverage.entrySet()) {

			collectedOrGaps = new HashSet<>();
			boolean foundNeighborWithoutGap = false;
			for (ReasoningNode neighbor : entry.getValue()) {
				// make sure neighbor has no knowledge gaps

				// knowledge engine specific code. We ignore meta knowledge interactions when
				// looking for knowledge gaps, because they are very generic and make finding
				// knowledge gaps nearly impossible.
				boolean isMeta = isMetaKI(neighbor);

				if (!isMeta && (someGaps = getKnowledgeGaps(neighbor)).isEmpty()) {
					// found neighbor without knowledge gaps for the current triple, so current
					// triple is covered.
					foundNeighborWithoutGap = true;
					break;
				}
				collectedOrGaps.addAll(someGaps);
			}

			if (!foundNeighborWithoutGap) {
				// there is a gap here, either in the current node or in a neighbor.

				if (collectedOrGaps.isEmpty()) {
					collectedOrGaps.add(new HashSet<>(Arrays.asList(entry.getKey())));
				}

				Set<Set<TriplePattern>> newExistingOrGaps = new HashSet<>();
				if (existingOrGaps.isEmpty()) {
					existingOrGaps.addAll(collectedOrGaps);
				} else {
					Set<TriplePattern> newGap;
					for (Set<TriplePattern> existingOrGap : existingOrGaps) {
						for (Set<TriplePattern> collectedOrGap : collectedOrGaps) {
							newGap = new HashSet<>();
							newGap.addAll(existingOrGap);
							newGap.addAll(collectedOrGap);
							newExistingOrGaps.add(newGap);
						}
					}
					existingOrGaps = newExistingOrGaps;
				}
			}
		}

		return existingOrGaps;
	}

	private static boolean isMetaKI(ReasoningNode neighbor) {

		BindingSetHandler bsh = neighbor.getRule().getBindingSetHandler();

		if (bsh instanceof ReactBindingSetHandler) {
			ReactBindingSetHandler rbsh = (ReactBindingSetHandler) bsh;
			return rbsh.getKnowledgeInteractionInfo().isMeta();
		} else if (bsh instanceof AnswerBindingSetHandler) {
			AnswerBindingSetHandler absh = (AnswerBindingSetHandler) bsh;
			return absh.getKnowledgeInteractionInfo().isMeta();
		}

		return false;
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
