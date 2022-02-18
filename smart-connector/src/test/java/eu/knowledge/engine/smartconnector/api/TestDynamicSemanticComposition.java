package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.AnswerBindingSetHandler;
import eu.knowledge.engine.smartconnector.impl.ReasonerProcessor.ReactBindingSetHandler;

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
		kn.addKB(kbTargetAttributeSupplier);

		LOG.info("Waiting for ready...");
		kn.startAndWaitForReady();

		// Patterns for the TargetObserver
		// an Answer pattern for Target observations
		GraphPattern gp1 = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		kbTargetObserver.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {
			assertTrue(
					anAnswerExchangeInfo.getIncomingBindings().isEmpty()
							|| anAnswerExchangeInfo.getIncomingBindings().iterator().next().getVariables().isEmpty(),
					"Should not have bindings in this binding set.");

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
		// and a post pattern to publish newly observed Targets
		PostKnowledgeInteraction postKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null);
		kbTargetObserver.register(postKI);

		// Patterns for the HVTSearcher
		// a pattern to ask for High Value Target searches
		GraphPattern gp2 = new GraphPattern(prefixes, "?id rdf:type v1905:HighValueTarget . ?id v1905:hasName ?name .");
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2);
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

		// Patterns for the TargetAttributeSupplier
		// a react pattern to get from targets to countries
		GraphPattern gp3in = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		GraphPattern gp3out = new GraphPattern(prefixes, "?id v1905:hasCountry ?country .");
		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3in, gp3out);
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

			LOG.info("resultBinding is {}", resultBindings);
			return resultBindings;
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
		kbTargetAttributeSupplier.setDomainKnowledge(ruleSet);

		kn.waitForUpToDate();

		// start testing ask for targets!
		BindingSet bindings = null;
		try {
			LOG.trace("Before ask.");
			AskResult result = kbHVTSearcher.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			LOG.trace("After ask.");
			// try to generate JSON tree.
			printSequenceDiagram(kbHVTSearcher.getKnowledgeBaseId().toString(), "ask", postKI.getArgument(),
					result.getReasoningNode());
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();
		LOG.info("Result bindings are: {}", bindings);

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertEquals("<https://www.tno.nl/target1>", b.get("id"), "Binding of 'id' is incorrect.");
//		assertEquals("\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>", b.get("name"), "Binding of 'name' is incorrect.");
		assertEquals("\"Bla\"", b.get("name"), "Binding of 'name' is incorrect.");

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding");

		// start testing post of targets!
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("id", "<https://www.tno.nl/target1>");
//		binding.put("name", "\"Bla\"^^<http://www.w3.org/2001/XMLSchema#string>");
		binding.put("name", "\"Bla\"");
		bindingSet.add(binding);

		try {
			LOG.info("Before post!");
			PostResult result = kbTargetObserver.post(postKI, bindingSet).get();

			bindings = result.getBindings();
			iter = bindings.iterator();
			assertFalse(iter.hasNext(), "there should be no bindings");
			LOG.info("After post!");

			// try to generate JSON tree.
			printSequenceDiagram(kbTargetObserver.getKnowledgeBaseId().toString(), "post", postKI.getArgument(),
					result.getReasoningNode());
		} catch (Exception e) {
			LOG.error("Error", e);
		}

	}

	private void printSequenceDiagram(String proactiveKB, String kiType, GraphPattern gp, ReasoningNode rn) {

//		System.out.println(rn.toString());

		Queue<ReasoningNode> queue = new LinkedList<ReasoningNode>();
		queue.add(rn);

		List<String> actors = new ArrayList<>();
		actors.add(proactiveKB);

		class Pair {
			String first;
			String second;

			public Pair(String aFirst, String aSecond) {
				first = aFirst;
				second = aSecond;
			}
		}

		Map<Pair, ReasoningNode> toFromExchanges = new HashMap<>();
		Map<Pair, ReasoningNode> toExchanges = new HashMap<>();

		while (!queue.isEmpty()) {

			ReasoningNode node = queue.poll();

			String currentActor = null;
			BindingSetHandler bsh = node.getRule().getBindingSetHandler();
			ReactBindingSetHandler rbsh = null;
			AnswerBindingSetHandler absh = null;
			if (bsh instanceof ReactBindingSetHandler) {
				rbsh = (ReactBindingSetHandler) bsh;

				currentActor = rbsh.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString();
				actors.add(currentActor);
			} else if (bsh instanceof AnswerBindingSetHandler) {
				absh = (AnswerBindingSetHandler) bsh;
				currentActor = absh.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString();
				actors.add(currentActor);
			} else {
				currentActor = proactiveKB;
			}

			for (ReasoningNode neighbor : node.getAntecedentNeighbors().keySet()) {
				BindingSetHandler bsh2 = neighbor.getRule().getBindingSetHandler();

				AnswerBindingSetHandler absh2 = null;
				ReactBindingSetHandler rbsh2 = null;
				if (bsh2 instanceof ReactBindingSetHandler) {
					rbsh2 = (ReactBindingSetHandler) bsh2;

					ReactKnowledgeInteraction react = (ReactKnowledgeInteraction) rbsh2.getKnowledgeInteractionInfo()
							.getKnowledgeInteraction();

					if (!react.isMeta()) {
						if (react.getResult() != null) {
							toFromExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						} else {
							toExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						}
					}

				} else if (bsh2 instanceof AnswerBindingSetHandler) {
					absh2 = (AnswerBindingSetHandler) bsh2;
					if (!absh2.getKnowledgeInteractionInfo().getKnowledgeInteraction().isMeta()) {
						toFromExchanges.put(new Pair(proactiveKB,
								absh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()), neighbor);
					}
				} else {
					toExchanges.put(new Pair(proactiveKB, proactiveKB), neighbor);
				}
			}

			for (ReasoningNode neighbor : node.getConsequentNeighbors().keySet()) {
				BindingSetHandler bsh2 = neighbor.getRule().getBindingSetHandler();

				AnswerBindingSetHandler absh2 = null;
				ReactBindingSetHandler rbsh2 = null;
				if (bsh2 instanceof ReactBindingSetHandler) {
					rbsh2 = (ReactBindingSetHandler) bsh2;

					ReactKnowledgeInteraction react = (ReactKnowledgeInteraction) rbsh2.getKnowledgeInteractionInfo()
							.getKnowledgeInteraction();

					if (react.isMeta()) {
						if (react.getResult() != null) {
							toFromExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						} else {
							toExchanges.put(
									new Pair(proactiveKB,
											rbsh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()),
									neighbor);
						}
					}
				} else if (bsh2 instanceof AnswerBindingSetHandler) {
					absh2 = (AnswerBindingSetHandler) bsh2;

					if (!absh2.getKnowledgeInteractionInfo().getKnowledgeInteraction().isMeta()) {
						toFromExchanges.put(new Pair(proactiveKB,
								absh2.getKnowledgeInteractionInfo().getKnowledgeBaseId().toString()), neighbor);
					}
				} else {
					toExchanges.put(new Pair(currentActor, currentActor), neighbor);
				}
			}

			queue.addAll(node.getAntecedentNeighbors().keySet());
			queue.addAll(node.getConsequentNeighbors().keySet());
		}

		String title = kiType + " data exchange";

		System.out.println("title " + title);

		for (String actor : actors) {
			System.out.println("actor " + removeChars(actor));
		}

		System.out.println("activate " + removeChars(proactiveKB));

		for (Pair pair : toExchanges.keySet()) {

			ReasoningNode node = toExchanges.get(pair);

			System.out.println(removeChars(pair.first) + "->" + removeChars(pair.second) + ":"
					+ checkSize(node.getBindingSetToHandler().toString()));

		}

		for (Pair pair : toFromExchanges.keySet()) {
			ReasoningNode node = toFromExchanges.get(pair);

			assert node != null;

			String toHandler = node.getBindingSetToHandler().toString();
			String fromHandler = node.getBindingSetFromHandler().toString();
			System.out.println(removeChars(pair.first) + "->" + removeChars(pair.second) + ":" + checkSize(toHandler));

			if (!pair.second.equals(proactiveKB))
				System.out.println("deactivate " + removeChars(proactiveKB));

			System.out.println("activate " + removeChars(pair.second));
			System.out
					.println(removeChars(pair.second) + "-->" + removeChars(pair.first) + ":" + checkSize(fromHandler));

			System.out.println("deactivate " + removeChars(pair.second));
			if (!pair.second.equals(proactiveKB))
				System.out.println("activate " + removeChars(proactiveKB));

		}

	}

	private String checkSize(String toHandler) {
		int endIndex = 100;
		if (toHandler.length() > endIndex)
			return "[{...}]";
		else
			return toHandler;
	}

	public static String removeChars(String path) {
		return path.replace(":", "");
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
