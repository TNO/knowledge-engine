package eu.interconnectproject.knowledge_engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interconnectproject.knowledge_engine.smartconnector.impl.Vocab;

public class TestRequestMetadata {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestRequestMetadata.class);

	@Test
	public void testRequestMetadata() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", Vocab.ONTO_URI);
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		final CountDownLatch latch = new CountDownLatch(1);

		kb1 = new MockedKnowledgeBase("kb1") {

			@Override
			public void smartConnectorReady(SmartConnector aSC) {

				GraphPattern gp = new GraphPattern(prefixes, "?obs rdf:type saref:Measurement .",
						"?obs saref:hasTemp ?temp .");
				PostKnowledgeInteraction ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null);
				aSC.register(ki);
			}
		};
		kb1.start();

		kb2 = new MockedKnowledgeBase("kb2") {

			private AskKnowledgeInteraction ki;

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes, "?kb rdf:type kb:KnowledgeBase .",
						"?kb kb:hasName ?name .", "?kb kb:hasDescription ?description .",
						"?kb kb:hasKnowledgeInteraction ?ki .", "?ki rdf:type ?kiType .", "?ki kb:isMeta ?isMeta .",
						"?ki kb:hasCommunicativeAct ?act .", "?act rdf:type kb:CommunicativeAct .",
						"?act kb:hasRequirement ?req .", "?act kb:hasSatisfaction ?sat .", "?req rdf:type ?reqType .",
						"?sat rdf:type ?satType .", "?ki kb:hasGraphPattern ?gp .", "?ki ?patternType ?gp .",
						"?gp rdf:type kb:GraphPattern .", "?gp kb:hasPattern ?pattern .");

				this.ki = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
				aSC.register(this.ki);

				try {
					this.testMetadata();
				} catch (InterruptedException | ExecutionException | ParseException e) {
					fail("Should not throw any exception.");
				}

			}

			public void testMetadata() throws InterruptedException, ExecutionException, ParseException {
				AskResult result = this.ask(this.ki, new BindingSet()).get();

				LOG.trace("Bindings: {}", result.getBindings());

				Model m = BindingSet.generateModel(this.ki.getPattern(), result.getBindings());

				List<Resource> i = m
						.listSubjectsWithProperty(RDF.type,
								ResourceFactory
										.createResource(prefixes.getNsPrefixURI("kb") + "PostKnowledgeInteraction"))
						.toList();
				assertEquals(3 + 1, i.size());
				latch.countDown();

			}
		};
		kb2.start();

		int wait = 20;
		assertTrue(latch.await(wait, TimeUnit.SECONDS), "Should execute the tests within " + wait + " seconds.");

	}

	@AfterAll
	public static void cleanup() {

		if (kb1 != null) {
			kb1.stop();
		}

		if (kb2 != null) {
			kb2.stop();
		}
	}
}
