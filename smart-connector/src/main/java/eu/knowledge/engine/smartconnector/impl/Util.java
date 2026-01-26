package eu.knowledge.engine.smartconnector.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.lang.arq.javacc.ParseException;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.api.GraphPattern;

public class Util {
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

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
						if (b.containsKey(n)) {
							newN = b.get(n);
						} else {
							LOG.error("The variable '{}' in the Knowledge should be bound.", n.getName());
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

	/**
	 * Translate bindingset from the internal KE/reasoner bindingsets to the API
	 * bindingset.
	 * 
	 * @param bs a reasoner bindingset
	 * @return a ke bindingset
	 */
	public static eu.knowledge.engine.smartconnector.api.BindingSet translateToApiBindingSet(BindingSet bs) {
		eu.knowledge.engine.smartconnector.api.BindingSet newBS = new eu.knowledge.engine.smartconnector.api.BindingSet();
		eu.knowledge.engine.smartconnector.api.Binding newB;

		SerializationContext context = new SerializationContext();
		context.setUsePlainLiterals(false);

		for (Binding b : bs) {
			newB = new eu.knowledge.engine.smartconnector.api.Binding();
			for (Map.Entry<Var, Node> entry : b.entrySet()) {
				newB.put(entry.getKey().getName(), FmtUtils.stringForNode(entry.getValue(), context));
			}
			newBS.add(newB);
		}
		return newBS;
	}

	/**
	 * Translate bindingset from the API bindingset to the internal KE/reasoner
	 * bindingset.
	 * 
	 * @param someBindings a ke bindingset
	 * @return a reasoner bindingset
	 */
	public static BindingSet translateFromApiBindingSet(
			eu.knowledge.engine.smartconnector.api.BindingSet someBindings) {

		BindingSet newBindingSet = new BindingSet();
		Binding newBinding;
		for (eu.knowledge.engine.smartconnector.api.Binding b : someBindings) {

			newBinding = new Binding();
			for (String var : b.getVariables()) {
				newBinding.put(var, b.get(var));
			}
			newBindingSet.add(newBinding);
		}

		return newBindingSet;
	}

}
