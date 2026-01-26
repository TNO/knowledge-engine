package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.javacc.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.smartconnector.impl.Util;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;

public class TestRequestMetadata {
	private static KnowledgeBaseImpl kb1;
	private static KnowledgeBaseImpl kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestRequestMetadata.class);
	private static KnowledgeNetwork kn;

	@Test
	public void testRequestMetadata() throws InterruptedException, ExecutionException, ParseException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		kn = new KnowledgeNetwork();

		kb1 = new KnowledgeBaseImpl("kb1");
		kn.addKB(kb1);

		kb2 = new KnowledgeBaseImpl("kb2");
		kn.addKB(kb2);

		GraphPattern gp = new GraphPattern(prefixes, "?obs rdf:type saref:Measurement .", "?obs saref:hasTemp ?temp .");
		PostKnowledgeInteraction ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null, false);
		kb1.register(ki);

		gp = new GraphPattern(prefixes, "?kb rdf:type kb:KnowledgeBase .", "?kb kb:hasName ?name .",
				"?kb kb:hasDescription ?description .", "?kb kb:hasKnowledgeInteraction ?ki .",
				"?ki rdf:type ?kiType .", "?ki kb:isMeta ?isMeta .", "?ki kb:hasCommunicativeAct ?act .",
				"?act rdf:type kb:CommunicativeAct .", "?act kb:hasRequirement ?req .",
				"?act kb:hasSatisfaction ?sat .", "?req rdf:type ?reqType .", "?sat rdf:type ?satType .",
				"?ki kb:hasGraphPattern ?gp .", "?gp rdf:type ?patternType .", "?gp kb:hasPattern ?pattern .");

		AskKnowledgeInteraction aKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp, true);
		kb2.register(aKI);

		kn.sync();
		LOG.info("Everyone is up-to-date!");

		AskResult result = kb2.ask(aKI, new BindingSet()).get();

		LOG.info("Bindings: {}", result.getBindings());

		Model m = Util.generateModel(aKI.getPattern(), Util.translateFromApiBindingSet(result.getBindings()));

		List<Resource> i = m
				.listSubjectsWithProperty(RDF.type,
						ResourceFactory.createResource(prefixes.getNsPrefixURI("kb") + "PostKnowledgeInteraction"))
				.toList();
		assertEquals(3 + 1, i.size());

		assertTrue(m.listStatements((Resource) null, RDF.type, Vocab.ARGUMENT_GRAPH_PATTERN).hasNext());

	}

	@AfterAll
	public static void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestRequestMetadata.class.getSimpleName());
		kn.stop().get();
	}
}
