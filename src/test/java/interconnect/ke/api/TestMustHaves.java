package interconnect.ke.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
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

import interconnect.ke.api.binding.Binding;
import interconnect.ke.api.binding.BindingSet;
import interconnect.ke.api.interaction.AskKnowledgeInteraction;
import interconnect.ke.api.interaction.PostKnowledgeInteraction;
import interconnect.ke.api.interaction.ReactKnowledgeInteraction;

public class TestMustHaves {

	private static final Logger LOG = LoggerFactory.getLogger(TestMustHaves.class);

	@Test
	public void requestKnowledgeBaseInfo() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("kb", "https://www.tno.nl/energy/ontology/interconnect/");
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		final CountDownLatch latch = new CountDownLatch(1);

		// the knowledge base from who we want to retrieve the data.
		KnowledgeBase kb1 = new TestKnowledgeBase("kb1") {

			@Override
			public void smartConnectorReady(SmartConnector aSC) {

				GraphPattern gp = new GraphPattern(prefixes, "?obs rdf:type saref:Measurement . ?obs :hasTemp ?temp .");
				PostKnowledgeInteraction ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null);
				aSC.register(ki);
			};
		};

		// the knowledge base who wants to retrieve the metadata of kb1.
		KnowledgeBase kb2 = new TestKnowledgeBase("kb2") {

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

	@Test
	public void testPostReact() throws InterruptedException {
		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");

		int wait = 2;
		final CountDownLatch kb2Initialized = new CountDownLatch(1);
		final CountDownLatch kb2ReceivedKnowledge = new CountDownLatch(1);

		// the knowledge base that posts the data.
		KnowledgeBase kb1 = new TestKnowledgeBase("kb1") {
			private PostKnowledgeInteraction ki;
			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes, "?a ex:b ?c.");
				this.ki = new PostKnowledgeInteraction(new CommunicativeAct(), gp, null);
				aSC.register(this.ki);

				waitForOtherKbAndPostSomething();
			};

			private void waitForOtherKbAndPostSomething() {
				// Wait until KB2 completed its latch.
				try {
					assertTrue(kb2Initialized.await(wait, TimeUnit.SECONDS),
							"kb2 should have been initialized within " + wait + " seconds.");
				} catch (InterruptedException e) {
					fail("Should not throw any exception.");
				}

				BindingSet bindingSet = new BindingSet();
				Binding binding = new Binding();
				binding.put("a", "<https://www.tno.nl/example/a>");
				binding.put("c", "<https://www.tno.nl/example/c>");
				bindingSet.add(binding);
				
				this.getSmartConnector().post(ki, bindingSet);
			}
		};

		// the knowledge base that receives the data
		KnowledgeBase kb2 = new TestKnowledgeBase("kb2") {
			@Override
			public void smartConnectorReady(SmartConnector aSC) {
				GraphPattern gp = new GraphPattern(prefixes, "?a ex:b ?c.");
				ReactKnowledgeInteraction ki = new ReactKnowledgeInteraction(new CommunicativeAct(), gp, null);

				aSC.register(ki, new ReactHandler() {
					@Override
					public BindingSet react(ReactKnowledgeInteraction anRKI, BindingSet argument) {
						Iterator<Binding> iter = argument.iterator();
						Binding b = iter.next();
						
						assertEquals("https://www.tno.nl/example/a", b.get("a"), "Binding of 'a' is incorrect.");
						assertEquals("https://www.tno.nl/example/c", b.get("c"), "Binding of 'c' is incorrect.");
						
						assertFalse(iter.hasNext(), "This BindingSet should only have a single binding.");

						// Complete the latch to make the test pass.
						kb2ReceivedKnowledge.countDown();

						return null;
					}
				});
			};
		};

		assertTrue(kb2ReceivedKnowledge.await(wait, TimeUnit.SECONDS), "Should execute the tests within " + wait + " seconds.");
	}

	@Test
	public void testCaseM1() {
		// TODO do we want to start a service directory?
		SmartConnector sc1 = TestUtils.getSmartConnector("kb1");
		SmartConnector sc2 = TestUtils.getSmartConnector("kb2");

		final int value = 23;

		PostKnowledgeInteraction pKI = new PostKnowledgeInteraction(new CommunicativeAct(),
				TestUtils.SAREF_MEASUREMENT_PATTERN, null);
		ReactKnowledgeInteraction rKI = new ReactKnowledgeInteraction(new CommunicativeAct(),
				TestUtils.SAREF_MEASUREMENT_PATTERN, null);

		sc1.register(pKI);
		sc2.register(rKI, new ReactHandler() {

			public BindingSet react(ReactKnowledgeInteraction aReactKnowledgeInteraction, BindingSet argument) {
				String value = argument.iterator().next().get("v");
				assertEquals(value, TestUtils.getIntegerValueFromString(value));
				return new BindingSet();
			}

		});

		sc1.post(pKI, TestUtils.getSingleBinding("v", TestUtils.getStringValueFromInteger(value), "m",
				TestUtils.getWithPrefix("m1")));

		sc2.stop();
		sc1.stop();
	}
}
