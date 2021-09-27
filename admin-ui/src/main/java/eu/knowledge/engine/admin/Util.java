package eu.knowledge.engine.admin;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.vocabulary.RDF;

import eu.knowledge.engine.smartconnector.api.Vocab;

public class Util {

	private static final String NONE = "<none>";
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

	public static String getGraphPattern(Model model, Resource kiRes) {
		Resource gpRes = kiRes.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasGraphPattern")));
		return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().toString();

	}

	public static String getArgument(Model model, Resource kiRes) {
		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasArgumentGraphPattern")));
		if (gpRes != null) {
			return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().toString();
		} else {
			return NONE;
		}
	}

	public static String getResult(Model model, Resource kiRes) {

		Resource gpRes = kiRes
				.getPropertyResourceValue(model.getProperty(prefixes.expandPrefix("kb:hasResultGraphPattern")));
		if (gpRes != null) {
			return gpRes.getProperty(model.getProperty(model.expandPrefix("kb:hasPattern"))).getObject().toString();
		} else {
			return NONE;
		}
	}

	public static String getProperty(Model m, Resource r, String propertyURI) {
		return r.getProperty(m.getProperty(propertyURI)).getObject().toString();
	}

}
