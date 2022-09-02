package eu.knowledge.engine.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.vocabulary.RDF;

import eu.knowledge.engine.admin.model.CommunicativeAct;
import eu.knowledge.engine.admin.model.Connection;
import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.RuleNode;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;
import eu.knowledge.engine.smartconnector.api.Vocab;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.AnswerBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactVoidBindingSetHandler;

public class Util {

	private static final String NONE = null;
	private static final PrefixMapping prefixes;
	static {
		// store some predefined prefixes
		prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");
	}

	// --------------------- RDF Model navigation helper methods ------------------

	public static Set<Resource> getKnowledgeBaseURIs(Model m) {

		ResIterator iter = m.listResourcesWithProperty(RDF.type,
				m.createResource(prefixes.expandPrefix("kb:KnowledgeBase")));

		Set<Resource> kbs = new HashSet<>();

		while (iter.hasNext()) {
			kbs.add(iter.next());
		}
		return kbs;
	}

	public static String getName(Model m, Resource r) {
		return getProperty(m, r, prefixes.expandPrefix("kb:hasName"));
	}

	public static String getDescription(Model m, Resource r) {
		return getProperty(m, r, prefixes.expandPrefix("kb:hasDescription"));
	}

	public static Set<Resource> getKnowledgeInteractionURIs(Model m, Resource r) {
		StmtIterator kiIter = m.listStatements(r, m.getProperty(prefixes.expandPrefix("kb:hasKnowledgeInteraction")),
				(RDFNode) null);

		Set<Resource> kis = new HashSet<>();

		while (kiIter.hasNext()) {
			kis.add(kiIter.next().getObject().asResource());
		}
		return kis;
	}

	public static String getKnowledgeInteractionType(Model m, Resource r) {
		return r.getPropertyResourceValue(RDF.type).getLocalName();
	}

	public static boolean isMeta(Model model, Resource kiRes) {
		return kiRes.getProperty(model.createProperty(prefixes.expandPrefix("kb:isMeta"))).getObject().asLiteral()
				.getBoolean();
	}

	public static CommunicativeAct getCommunicativeAct(Model model, Resource kiRes) {
		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasCommunicativeAct")));
		CommunicativeAct ca = new CommunicativeAct();
		if (gpRes != null) {
			StmtIterator reqIter = model.listStatements(gpRes,
					model.getProperty(prefixes.expandPrefix("kb:hasRequirement")), (RDFNode) null);
			while (reqIter.hasNext()) {
				ca.addRequiredPurposesItem(reqIter.next().getObject().toString());
			}
			StmtIterator satIter = model.listStatements(gpRes,
					model.getProperty(prefixes.expandPrefix("kb:hasSatisfaction")), (RDFNode) null);
			while (satIter.hasNext()) {
				ca.addSatisfiedPurposesItem(satIter.next().getObject().toString());
			}

		}
		return ca;
	}

	public static String getGraphPattern(Model model, Resource kiRes) {
		Resource gpRes = kiRes.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasGraphPattern")));
		return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().asLiteral()
				.getLexicalForm();

	}

	public static String getArgument(Model model, Resource kiRes) {
		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasArgumentGraphPattern")));
		if (gpRes != null) {
			return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().asLiteral()
					.getLexicalForm();
		} else {
			return NONE;
		}
	}

	public static String getResult(Model model, Resource kiRes) {
		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasResultGraphPattern")));
		if (gpRes != null) {
			return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().asLiteral()
					.getLexicalForm();
		} else {
			return NONE;
		}
	}

	public static String getProperty(Model m, Resource r, String propertyURI) {
		return r.getProperty(m.getProperty(propertyURI)).getObject().toString();
	}

	public static List<Connection> createConnectionObjects(RuleNode rn) {

		System.out.println(rn.toString());

		Queue<RuleNode> queue = new LinkedList<RuleNode>();
		queue.add(rn);

		Set<String> actors = new HashSet<>();

		while (!queue.isEmpty()) {

			RuleNode node = queue.poll();

			String currentActor = null;

			if (!(node.getRule() instanceof ProactiveRule)) {

				Rule rule = (Rule) node.getRule();
				BindingSetHandler bsh = rule.getBindingSetHandler();

				if (bsh != null) {
					ReactBindingSetHandler rbsh = null;
					AnswerBindingSetHandler absh = null;
					if (bsh instanceof ReactBindingSetHandler) {
						rbsh = (ReactBindingSetHandler) bsh;
						currentActor = rbsh.getKnowledgeInteractionInfo().getId().toString();
						actors.add(currentActor);
					} else if (bsh instanceof AnswerBindingSetHandler) {
						absh = (AnswerBindingSetHandler) bsh;
						currentActor = absh.getKnowledgeInteractionInfo().getId().toString();
						actors.add(currentActor);
					}
				} else {
					assert rule.getConsequent().isEmpty();
					SinkBindingSetHandler sbsh = rule.getSinkBindingSetHandler();
					ReactVoidBindingSetHandler rvbsh = null;
					if (sbsh instanceof ReactVoidBindingSetHandler) {
						rvbsh = (ReactVoidBindingSetHandler) sbsh;
						currentActor = rvbsh.getKnowledgeInteractionInfo().getId().toString();
						actors.add(currentActor);
					}
				}
			}
			queue.addAll(node.getAntecedentNeighbors().keySet());
			queue.addAll(node.getConsequentNeighbors().keySet());
		}

		List<Connection> connections = new ArrayList<Connection>();
		for (String actor : actors) {
			connections.add(new Connection().knowledgeInteractionId(actor));
		}
		return connections;
	}
}
