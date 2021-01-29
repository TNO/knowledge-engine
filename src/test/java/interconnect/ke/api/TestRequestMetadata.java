package interconnect.ke.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;

public class TestRequestMetadata {
	private static MockedKnowledgeBase kb1;
	private static MockedKnowledgeBase kb2;

	private static final Logger LOG = LoggerFactory.getLogger(TestRequestMetadata.class);

	@Test
	public void testRequestMetadata() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect#");
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		final CountDownLatch latch = new CountDownLatch(1);

		kb1 = new MockedKnowledgeBase("kb1") {

			@Override
			public void smartConnectorReady(SmartConnector aSC) {

				GraphPattern gp = new GraphPattern(prefixes,
						"?obs rdf:type saref:Measurement . ?obs saref:hasTemp ?temp .");
				PostKnowledgeInteraction ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null);
				aSC.register(ki);
			}
		};

		kb2 = new MockedKnowledgeBase("kb2") {

			private AskKnowledgeInteraction ki;

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes,
						"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasDescription ?description . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType . ?ki kb:isMeta ?isMeta . ?ki kb:hasGraphPattern ?gp . ?ki ?patternType ?gp . ?gp rdf:type kb:GraphPattern . ?gp kb:hasPattern ?pattern .");
				this.ki = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
				aSC.register(this.ki);

				try {
					this.testMetadata();
				} catch (InterruptedException | ExecutionException | ParseException e) {
					fail("Should not throw any exception.");
				}

			}

			public void testMetadata() throws InterruptedException, ExecutionException, ParseException {
				AskResult result = this.getSmartConnector().ask(this.ki, new BindingSet()).get();

				LOG.info("Bindings: {}", result.getBindings());

				Model m = BindingSet.generateModel(this.ki.getPattern(), result.getBindings());

				ResIterator i = m.listSubjectsWithProperty(RDF.type,
						ResourceFactory.createResource(prefixes.getNsPrefixURI("kb") + "PostKnowledgeInteraction"));
				assertTrue(i.hasNext(), "Should have at least 1 PostKnowledgeInteraction.");
				i.next();
				assertTrue(!i.hasNext(), "Should have at most 1 PostKnowledgeInteraction.");
				latch.countDown();

			}
		};

		int wait = 2;
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
