package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class TestDynamicSemanticComposition {

	private static final Logger LOG = LoggerFactory.getLogger(TestDynamicSemanticComposition.class);

	private static MockedKnowledgeBase kbTargetObserver;
	private static MockedKnowledgeBase kbHVTSearcher;
	private static MockedKnowledgeBase kbTargetAttributeSupplier;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testAskAnswer() throws InterruptedException, URISyntaxException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");
//		prefixes.setNsPrefix("tno", "https://www.tno.nl/");
		prefixes.setNsPrefix("v1905", "https://www.tno.nl/defense/ontology/v1905/");

		var kn = new KnowledgeNetwork();
		// start a knowledge base with the behaviour "I can supply observations of
		// targets"
		kbTargetObserver = new MockedKnowledgeBase("TargetObserver");
		kbTargetObserver.setReasonerEnabled(true);
		kn.addKB(kbTargetObserver);
		// start a knowledge base with the behaviour "I am interested in high-value
		// targets"
		kbHVTSearcher = new MockedKnowledgeBase("HVTSearcher");
		kbHVTSearcher.setReasonerEnabled(true);
		kn.addKB(kbHVTSearcher);
		// start a knowledge base with the behaviour "Give me a target and I can supply
		// its basic attributes"
		kbTargetAttributeSupplier = new MockedKnowledgeBase("TargetAttributeSupplier");
		kbTargetAttributeSupplier.setReasonerEnabled(true);

		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		// Patterns for the TargetObserver
		// an Answer pattern for Target observations
		GraphPattern gp1 = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1, "answerTargets");
		kbTargetObserver.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

			// add 2 dummy bindings to the answer
			BindingSet bindingSet = new BindingSet();
			Binding binding1 = new Binding();
			binding1.put("id", "<https://www.tno.nl/example/target0>");
			binding1.put("name", "\"Eek\"^^<http://www.w3.org/2001/XMLSchema#string>");
			bindingSet.add(binding1);
			Binding binding2 = new Binding();
			binding2.put("id", "<https://www.tno.nl/example/target1>");
			binding2.put("name", "\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>");
			bindingSet.add(binding2);

			return bindingSet;
		});
		// and a post pattern to publish newly observed Targets
		PostKnowledgeInteraction postKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null,
				"postTargets");
		kbTargetObserver.register(postKI);

		// Patterns for the HVTSearcher
		// a pattern to ask for High Value Target searches
		GraphPattern gp2 = new GraphPattern(prefixes, "?id rdf:type v1905:HighValueTarget . ?id v1905:hasName ?name .");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2, "askHVTargets");
		kbHVTSearcher.register(askKI);
		// a pattern to react to incoming new High Value Targets
		ReactKnowledgeInteraction reactKIsearcher = new ReactKnowledgeInteraction(new CommunicativeAct(), gp2, null);
		kbHVTSearcher.register(reactKIsearcher, (anRKI, aReactExchangeInfo) -> {

			LOG.info("HVT Searcher reacting to incoming HVTs...");
			var argument = aReactExchangeInfo.getArgumentBindings();
			Iterator<Binding> iter = argument.iterator();
			while (iter.hasNext()) {
				Binding b = iter.next();
				LOG.info("Incoming HVT is {}", b);
			}
			return null;
		});

		// add extra domain knowledge in the form of a rule to kbHVTSearcher.
		HashSet<TriplePattern> consequent = new HashSet<TriplePattern>();
		consequent.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/HighValueTarget>"));
		HashSet<TriplePattern> antecedent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/Target>"));
		antecedent.add(new TriplePattern("?id <https://www.tno.nl/defense/ontology/v1905/hasCountry> \"Russia\""));
		Rule r = new Rule(antecedent, consequent);
		Set<Rule> ruleSet = new HashSet<>();
		ruleSet.add(r);
		kbHVTSearcher.setDomainKnowledge(ruleSet);
		kbTargetObserver.setDomainKnowledge(ruleSet);

		kn.waitForUpToDate();

		// first check if our plan is covered
		BindingSet bindings = null;
		try {
			AskPlan askPlan = kbHVTSearcher.planAsk(askKI, new RecipientSelector());

			ReasoningNode plan = askPlan.getReasoningNode();

			Set<TriplePattern> knowledgeGaps = plan.getKnowledgeGaps();
			LOG.info("Not satisfied triple patterns: {}", knowledgeGaps);

			AskResult result = askPlan.execute(new BindingSet()).get();
			bindings = result.getBindings();
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		// Patterns for the TargetAttributeSupplier
		kn.addKB(kbTargetAttributeSupplier);
		kbTargetAttributeSupplier.setDomainKnowledge(ruleSet);
		// a react pattern to get from targets to countries
		GraphPattern gp3in = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		GraphPattern gp3out = new GraphPattern(prefixes, "?id v1905:hasCountry ?country .");
		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3in, gp3out,
				"reactCountry");
		kbTargetAttributeSupplier.register(reactKI, (anRKI, aReactExchangeInfo) -> {

			LOG.info("TargetAttributeSupplier Reacting...");
			var argument = aReactExchangeInfo.getArgumentBindings();
			LOG.info("Argument bindings are: {}", argument);
			Iterator<Binding> iter = argument.iterator();

			BindingSet resultBindings = new BindingSet();

			while (iter.hasNext()) {
				Binding b = iter.next();
				String id = b.get("id");
				String country = "";
				Binding rb = new Binding();
				rb.put("id", id);
				if (b.get("id").equals("<https://www.tno.nl/example/target1>")) {
					country = "\"Russia\"";
				} else if (b.get("id").equals("<https://www.tno.nl/example/target0>")) {
					country = "\"Holland\"";
				} else {
					country = "\"Belgium\"";
				}
				rb.put("country", country);
				resultBindings.add(rb);
			}

			LOG.info("resultBinding is {}", resultBindings);
			return resultBindings;
		});

		// start testing ask for targets!
		BindingSet bindings2 = null;
		try {
			LOG.trace("Before ask.");
			AskPlan askPlan = kbHVTSearcher.planAsk(askKI, new RecipientSelector());

			ReasoningNode plan = askPlan.getReasoningNode();

			Set<TriplePattern> notSatisfiedTriplePatterns = new HashSet<>();
			Map<TriplePattern, Set<ReasoningNode>> nodeCoverage = plan
					.findAntecedentCoverage(plan.getAntecedentNeighbors());
			for (Entry<TriplePattern, Set<ReasoningNode>> entry : nodeCoverage.entrySet()) {
				if (entry.getValue().isEmpty()) {
					notSatisfiedTriplePatterns.add(entry.getKey());
				}
			}

			LOG.info("Not satisfied triple patterns: {}", notSatisfiedTriplePatterns);

			AskResult result = askPlan.execute(new BindingSet()).get();
			bindings2 = result.getBindings();

			LOG.trace("After ask.");
			// try to generate JSON tree.
			TestUtils.printSequenceDiagram(kbHVTSearcher.getKnowledgeBaseId().toString(), "ask", postKI.getArgument(),
					result.getReasoningNode(), prefixes);
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings2.iterator();
		LOG.info("Result bindings are: {}", bindings2);

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertEquals("<https://www.tno.nl/example/target1>", b.get("id"), "Binding of 'id' is incorrect.");
//		assertEquals("\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>", b.get("name"), "Binding of 'name' is incorrect.");
		assertEquals("\"Bla\"", b.get("name"), "Binding of 'name' is incorrect.");

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding");

		// start testing post of targets!
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("id", "<https://www.tno.nl/example/target1>");
//		binding.put("name", "\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>");
		binding.put("name", "\"Bla\"");
		bindingSet.add(binding);

		try {
			LOG.info("Before post!");
			PostPlan aPlan = kbTargetObserver.planPost(postKI, new RecipientSelector());
			PostResult result = aPlan.execute(bindingSet).get();
			bindings2 = result.getBindings();
			iter = bindings2.iterator();
			assertFalse(iter.hasNext(), "there should be no bindings");
			LOG.info("After post!");

//			TestUtils.printReasoningNodeDotNotation("TargetObserver", aPlan.getReasoningNode());

			// try to generate JSON tree.
			TestUtils.printSequenceDiagram(kbTargetObserver.getKnowledgeBaseId().toString(), "post",
					postKI.getArgument(), result.getReasoningNode(), prefixes);
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
