package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.api.AnswerHandler;
import eu.knowledge.engine.smartconnector.api.AnswerKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskKnowledgeInteraction;
import eu.knowledge.engine.smartconnector.api.AskResult;
import eu.knowledge.engine.smartconnector.api.Binding;
import eu.knowledge.engine.smartconnector.api.BindingSet;
import eu.knowledge.engine.smartconnector.api.CommunicativeAct;
import eu.knowledge.engine.smartconnector.api.GraphPattern;

public class TestDynamicSemanticComposition {

	private static final Logger LOG = LoggerFactory.getLogger(TestDynamicSemanticComposition.class);

	private static MockedKnowledgeBase kbTargetObserver;
	private static MockedKnowledgeBase kbHVTSearcher;
	private static MockedKnowledgeBase kbTargetAttributeSupplier;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");
		prefixes.setNsPrefix("v1905", "https://www.tno.nl/defense/ontology/v1905/");

		var kn = new KnowledgeNetwork();
		// start a knowledge base with the behaviour "I can supply observations of targets"
		kbTargetObserver = new MockedKnowledgeBase("TargetObserver");
		kn.addKB(kbTargetObserver);
		// start a knowledge base with the behaviour "I am interested in high-value targets"
		kbHVTSearcher = new MockedKnowledgeBase("HVTSearcher");
		kn.addKB(kbHVTSearcher);
		// start a knowledge base with the behaviour "Give me a target and I can supply its basic attributes"
		kbTargetAttributeSupplier = new MockedKnowledgeBase("TargetAttributeSupplier");
		kn.addKB(kbTargetAttributeSupplier);

		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		// a pattern for Target observations
		GraphPattern gp1 = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kbTargetObserver.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(anAnswerExchangeInfo.getIncomingBindings().isEmpty() || anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(), "Should not have bindings in this binding set.");
			
			// add 2 dummy bindings to the answer
			BindingSet bindingSet = new BindingSet();
			Binding binding1 = new Binding();
			binding1.put("id", "<https://www.tno.nl/target0>");
			binding1.put("name", "\"Eek\"^^<http://www.w3.org/2001/XMLSchema#string>");
			bindingSet.add(binding1);
			Binding binding2 = new Binding();
			binding2.put("id", "<https://www.tno.nl/target1>");
			binding2.put("name", "\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>");
			bindingSet.add(binding2);

			return bindingSet;
		});

		// a pattern for High Value Target searches
		GraphPattern gp2 = new GraphPattern(prefixes, "?id rdf:type v1905:HighValueTarget . ?id v1905:hasName ?name .");
		//GraphPattern gp2 = new GraphPattern(prefixes, "?idHVT rdf:type v1905:Target . ?idHVT v1905:hasName ?nameHVT .");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
		kbHVTSearcher.register(askKI);

		// a post pattern and a react pattern to get from targets to countries
		GraphPattern gp3in = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		GraphPattern gp3out = new GraphPattern(prefixes, "?id v1905:hasCountry ?country .");
		PostKnowledgeInteraction postKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp3in, gp3out);
		kbHVTSearcher.register(postKI);
		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3in, gp3out);
		kbTargetAttributeSupplier.register(reactKI, (anRKI, aReactExchangeInfo) -> {

			LOG.info("TargetAttributeSupplier Reacting...");
			var argument = aReactExchangeInfo.getArgumentBindings();
			LOG.info("Arguments bindings are: {}", argument);
			Iterator<Binding> iter = argument.iterator();

			BindingSet resultBindings = new BindingSet();
			
			while (iter.hasNext()) {
				Binding b = iter.next();
				String id = b.get("id");
				String country = "";
				Binding rb = new Binding();
				rb.put("id", id);
				if (b.get("id").equals("<https://www.tno.nl/target1>")) {
					country = "\"Russia\"";
				} else if (b.get("id").equals("<https://www.tno.nl/target0>")) {
					country = "\"Holland\"";
				} else {
					country = "\"Belgium\"";
				}
				rb.put("country", country);
				resultBindings.add(rb);
			}
			
			LOG.info("resultBinding is {}",resultBindings);			
			return resultBindings;
		});
		
		// add extra domain knowledge in the form of a rule to kbHVTSearcher. 
        HashSet<TriplePattern> consequent = new HashSet<TriplePattern>();
        consequent.add(new TriplePattern("?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/HighValueTarget>"));
        HashSet<TriplePattern> antecedent = new HashSet<TriplePattern>();
        antecedent.add(new TriplePattern("?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/Target>"));
        antecedent.add(new TriplePattern("?id <https://www.tno.nl/defense/ontology/v1905/hasCountry> \"Russia\""));
        Rule r = new Rule(antecedent, consequent);
        Set<Rule> ruleSet = new HashSet<>();
        ruleSet.add(r);
        kbHVTSearcher.setDomainKnowledge(ruleSet);
				
		kn.waitForUpToDate();

		// start testing ask for targets!
		BindingSet bindings = null;
		try {
			LOG.trace("Before ask.");
			AskResult result = kbHVTSearcher.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();
		LOG.info("Result bindings are: {}",bindings);

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertEquals("<https://www.tno.nl/target1>", b.get("id"), "Binding of 'id' is incorrect.");
		assertEquals("\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>", b.get("name"), "Binding of 'name' is incorrect.");

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding");
		
		// start testing post of target and react with country!
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("id", "<https://www.tno.nl/target0>");
		binding.put("name", "\"Eek\"^^<http://www.w3.org/2001/XMLSchema#string>");
		bindingSet.add(binding);

		try {
			PostResult result = kbHVTSearcher.post(postKI, bindingSet).get();
			bindings = result.getBindings();
			iter = bindings.iterator();

			assertTrue(iter.hasNext(), "there should be at least 1 binding");
			b = iter.next();

			assertEquals("<https://www.tno.nl/target0>", b.get("id"), "Binding of 'id' is incorrect.");
			assertEquals("\"Holland\"", b.get("country"), "Binding of 'country' is incorrect.");

			assertFalse(iter.hasNext(), "This BindingSet should only have a single binding");
			
			LOG.info("After post!");
		} catch (Exception e) {
			LOG.error("Error", e);
		}

	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestDynamicSemanticComposition.class.getSimpleName());
		if (kbTargetObserver != null) {
			kbTargetObserver.stop();
		} else {
			fail("kbTargetObserver should not be null!");
		}

		if (kbHVTSearcher != null) {
			kbHVTSearcher.stop();
		} else {
			fail("kbHVTSearcher should not be null!");
		}

		if (kbTargetAttributeSupplier != null) {
			kbTargetAttributeSupplier.stop();
		} else {
			fail("kbTargetAttributeSupplier should not be null!");
		}
}
}
