package interconnect.ke.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;

public class TestRequestMetadata {

	private static final Logger LOG = LoggerFactory.getLogger(TestRequestMetadata.class);

	@Test
	public void testRequestMetadata() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect/");
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		final CountDownLatch latch = new CountDownLatch(1);

		// the knowledge base from who we want to retrieve the data.
		KnowledgeBase kb1 = new MockedKnowledgeBase("kb1") {

			@Override
			public void smartConnectorReady(SmartConnector aSC) {

				GraphPattern gp = new GraphPattern(prefixes, "?obs rdf:type saref:Measurement . ?obs :hasTemp ?temp .");
				PostKnowledgeInteraction ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null);
				aSC.register(ki);
			};
		};

		// the knowledge base who wants to retrieve the metadata of kb1.
		KnowledgeBase kb2 = new MockedKnowledgeBase("kb2") {

			private AskKnowledgeInteraction ki;

			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes,
						"?kb rdf:type kb:KnowledgeBase . ?kb kb:hasName ?name . ?kb kb:hasKnowledgeInteraction ?ki . ?ki rdf:type ?kiType .");
				ki = new AskKnowledgeInteraction(new CommunicativeAct(), gp);
				aSC.register(ki);

				try {
					this.testMetadata();
				} catch (InterruptedException | ExecutionException | ParseException e) {
					fail("Should not throw any exception.");
				}

			};

			public void testMetadata() throws InterruptedException, ExecutionException, ParseException {
				AskResult result = this.getSmartConnector().ask(this.ki, new BindingSet()).get();

				Model m = TestUtils.generateModel(this.ki.getPattern(), result.getBindings());

				ResIterator i = m.listSubjectsWithProperty(RDF.type,
						prefixes.getNsPrefixURI("ke") + "PostKnowledgeInteraction");
				assertTrue(i.hasNext(), "Should have exactly 1 PostKnowledgeInteraction.");
				latch.countDown();

			}
		};

		int wait = 2;
		assertTrue(latch.await(wait, TimeUnit.SECONDS), "Should execute the tests within " + wait + " seconds.");
	}
}
