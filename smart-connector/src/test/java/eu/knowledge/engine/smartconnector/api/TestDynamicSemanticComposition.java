package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule.MatchFlag;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestDynamicSemanticComposition {

	private static final Logger LOG = LoggerFactory.getLogger(TestDynamicSemanticComposition.class);

	private KnowledgeBaseImpl kbTargetObserver;
	private KnowledgeBaseImpl kbHVTSearcher;
	private KnowledgeBaseImpl kbTargetCountrySupplier;
	private KnowledgeBaseImpl kbTargetLanguageSupplier;
	private KnowledgeBaseImpl kbTargetAgeSupplier;

	private KnowledgeNetwork kn;

	private PrefixMappingMem prefixes;

	private AskKnowledgeInteraction askKI;
	private PostKnowledgeInteraction postKI;

	private Set<Rule> ruleSet;

	@BeforeEach
	public void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {

		prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");
//		prefixes.setNsPrefix("tno", "https://www.tno.nl/");
		prefixes.setNsPrefix("v1905", "https://www.tno.nl/defense/ontology/v1905/");

		// add extra domain knowledge in the form of a rule to kbHVTSearcher.
		HashSet<TriplePattern> consequent1 = new HashSet<TriplePattern>();
		consequent1.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/HighValueTarget>"));
		HashSet<TriplePattern> antecedent1 = new HashSet<TriplePattern>();
		antecedent1.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/Target>"));
		antecedent1.add(new TriplePattern("?id <https://www.tno.nl/defense/ontology/v1905/hasCountry> \"Russia\""));

		HashSet<TriplePattern> consequent2 = new HashSet<TriplePattern>();
		consequent2.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/HighValueTarget>"));
		HashSet<TriplePattern> antecedent2 = new HashSet<TriplePattern>();
		antecedent2.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/Target>"));
		antecedent2.add(new TriplePattern("?id <https://www.tno.nl/defense/ontology/v1905/hasLanguage> \"Russian\""));

		HashSet<TriplePattern> consequent3 = new HashSet<TriplePattern>();
		consequent3.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/HighValueTarget>"));
		HashSet<TriplePattern> antecedent3 = new HashSet<TriplePattern>();
		antecedent3.add(new TriplePattern(
				"?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://www.tno.nl/defense/ontology/v1905/Target>"));
		antecedent3.add(new TriplePattern("?id <https://www.tno.nl/defense/ontology/v1905/hasLanguage> \"Russian\""));
		antecedent3.add(new TriplePattern("?id <https://www.tno.nl/defense/ontology/v1905/hasCountry> \"Russia\""));

		ruleSet = new HashSet<>();
		ruleSet.add(new Rule(antecedent1, consequent1));
		ruleSet.add(new Rule(antecedent2, consequent2));
//		ruleSet.add(new Rule(antecedent3, consequent3));

	}

//	@RepeatedTest(100)
	@Test
	public void testAskAnswer() throws InterruptedException, URISyntaxException {

		setupNetwork();

		// perform an ASK that should produce gaps and if found update network to fix gaps
		try {
			AskResult resultWithGaps = kbHVTSearcher.ask(askKI, new BindingSet()).get();
			// check for knowledge gaps
			Set<KnowledgeGap> gaps = resultWithGaps.getKnowledgeGaps();		
			LOG.info("Found gaps: " + gaps);
			// add KB that fills the knowledge gap
			updateNetwork(gaps);
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		// perform ask for targets and knowledge gaps should have been fixed
		BindingSet bindings = null;
		try {
			AskResult result = kbHVTSearcher.ask(askKI, new BindingSet()).get();
			bindings = result.getBindings();
			// try to generate JSON tree.
			TestUtils.printSequenceDiagram(kbHVTSearcher.getKnowledgeBaseId().toString(), "ask", postKI.getArgument(),
					result.getReasonerPlan(), prefixes);
		} catch (InterruptedException | ExecutionException e) {
			fail();
		}

		Iterator<Binding> iter = bindings.iterator();
		LOG.info("Result bindings are: {}", bindings);

		assertTrue(iter.hasNext(), "there should be at least 1 binding");
		Binding b = iter.next();

		assertEquals("<https://www.tno.nl/example/target1>", b.get("id"), "Binding of 'id' is incorrect.");
		assertEquals("\"Bla\"", b.get("name"), "Binding of 'name' is incorrect.");

		assertFalse(iter.hasNext(), "This BindingSet should only have a single binding");

		// start testing post of targets!
		BindingSet bindingSet = new BindingSet();
		Binding binding = new Binding();
		binding.put("id", "<https://www.tno.nl/example/target1>");
		binding.put("name", "\"Bla\"");
		bindingSet.add(binding);

		try {
			LOG.info("Before post!");
			PostPlan aPlan = kbTargetObserver.planPost(postKI, new RecipientSelector());
			PostResult result = aPlan.execute(bindingSet).get();
			bindings = result.getBindings();
			iter = bindings.iterator();
			assertFalse(iter.hasNext(), "there should be no bindings");
			LOG.info("After post!");

			aPlan.getReasonerPlan().getStore().printGraphVizCode(aPlan.getReasonerPlan());

			// try to generate JSON tree.
			TestUtils.printSequenceDiagram(kbTargetObserver.getKnowledgeBaseId().toString(), "post",
					postKI.getArgument(), result.getReasonerPlan(), prefixes);
		} catch (Exception e) {
			LOG.error("Error", e);
			fail(e);
		}
	}

	private void setupNetwork() {

		instantiateHVTSearcherKB();

		instantiateObserverKB();

		kn = new KnowledgeNetwork();
		kn.addKB(kbTargetObserver);
		kn.addKB(kbHVTSearcher);

		long start = System.nanoTime();
		kn.sync();
		long end = System.nanoTime();
		LOG.info("Duration: {}", (((double) (end - start)) / 1_000_000));
	}

	private void updateNetwork(Set<KnowledgeGap> gaps) {

		instantiateTargetCountrySupplierKB();
		instantiateTargetLanguageSupplierKB();
		instantiateTargetAgeSupplierKB();

		List<KnowledgeBaseImpl> availableKBs = new ArrayList<>();
		availableKBs.add(kbTargetAgeSupplier);
		availableKBs.add(kbTargetCountrySupplier);
		availableKBs.add(kbTargetLanguageSupplier);

		// add the first KB that fulfills the gap
		kbs: for (KnowledgeBaseImpl kb : availableKBs) {

			for (ReactKnowledgeInteraction ki : kb.getReactKnowledgeInteractions().keySet()) {

				for (Set<TriplePattern> gap : gaps) {

					Rule r = new Rule(translateGraphPatternTo(ki.getArgument()),
							translateGraphPatternTo(ki.getResult()));

					Set<Match> matches = r.consequentMatches(gap, EnumSet.of(MatchFlag.ONLY_BIGGEST));

					if (!matches.isEmpty()) {
						kn.addKB(kb);
						break kbs;
					}
				}
			}
		}

//		kn.addKB(kbTargetCountrySupplier);
//		kn.addKB(kbTargetLanguageSupplier);
//		kn.addKB(kbTargetAgeSupplier);

		kn.sync();
	}

	private Set<TriplePattern> translateGraphPatternTo(GraphPattern pattern) {

		TriplePattern tp;
		TriplePath triplePath;
		String triple;
		ElementPathBlock epb = pattern.getGraphPattern();
		Iterator<TriplePath> iter = epb.patternElts();

		Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();

		while (iter.hasNext()) {

			triplePath = iter.next();

			triple = FmtUtils.stringForTriple(triplePath.asTriple(), new PrefixMappingZero());

			tp = new TriplePattern(triple);
			triplePatterns.add(tp);
		}

		return triplePatterns;
	}

	public void instantiateHVTSearcherKB() {
		// start a knowledge base with the behaviour "I am interested in high-value
		// targets"
		kbHVTSearcher = new KnowledgeBaseImpl("HVTSearcher");
		kbHVTSearcher.setReasonerEnabled(true);

		// Patterns for the HVTSearcher
		// a pattern to ask for High Value Target searches
		GraphPattern gp2 = new GraphPattern(prefixes, "?id rdf:type v1905:HighValueTarget . ?id v1905:hasName ?name .");
		this.askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2, "askHVTargets",true);
		kbHVTSearcher.register(this.askKI);
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
			return new BindingSet();
		});

		kbHVTSearcher.setDomainKnowledge(ruleSet);
	}

	public void instantiateObserverKB() {
		// start a knowledge base with the behaviour "I can supply observations of
		// targets"
		kbTargetObserver = new KnowledgeBaseImpl("TargetObserver");
		kbTargetObserver.setReasonerEnabled(true);

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
		this.postKI = new PostKnowledgeInteraction(new CommunicativeAct(), gp1, null, "postTargets");
		kbTargetObserver.register(this.postKI);

		kbTargetObserver.setDomainKnowledge(ruleSet);
	}

	public void instantiateTargetLanguageSupplierKB() {
		kbTargetLanguageSupplier = new KnowledgeBaseImpl("TargetLanguageSupplier");
		kbTargetLanguageSupplier.setReasonerEnabled(true);

		// Patterns for the TargetLanguageSupplier
		// a react pattern to get from targets to language
		GraphPattern gp4in = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		GraphPattern gp4out = new GraphPattern(prefixes, "?id v1905:hasLanguage ?lang .");
		ReactKnowledgeInteraction reactKI2 = new ReactKnowledgeInteraction(new CommunicativeAct(), gp4in, gp4out,
				"reactLanguage");
		kbTargetLanguageSupplier.register(reactKI2, (anRKI, aReactExchangeInfo) -> {

			LOG.info("TargetLanguageSupplier Reacting...");
			var argument = aReactExchangeInfo.getArgumentBindings();
			LOG.info("Argument bindings are: {}", argument);
			Iterator<Binding> iter = argument.iterator();

			BindingSet resultBindings = new BindingSet();

			while (iter.hasNext()) {
				Binding b = iter.next();
				String id = b.get("id");
				String language = "";
				Binding rb = new Binding();
				rb.put("id", id);
				if (b.get("id").equals("<https://www.tno.nl/example/target1>")) {
					language = "\"Russian\"";
				} else if (b.get("id").equals("<https://www.tno.nl/example/target0>")) {
					language = "\"Dutch\"";
				} else {
					language = "\"Flemish\"";
				}
				rb.put("lang", language);
				resultBindings.add(rb);
			}

			LOG.info("resultBinding is {}", resultBindings);
			return resultBindings;
		});
		kbTargetLanguageSupplier.setDomainKnowledge(ruleSet);
	}

	public void instantiateTargetAgeSupplierKB() {
		kbTargetAgeSupplier = new KnowledgeBaseImpl("TargetAgeSupplier");
		kbTargetAgeSupplier.setReasonerEnabled(true);
	}

	public void instantiateTargetCountrySupplierKB() {
		// start a knowledge base with the behaviour "Give me a target and I can supply
		// its basic attributes"
		kbTargetCountrySupplier = new KnowledgeBaseImpl("TargetCountrySupplier");
		kbTargetCountrySupplier.setReasonerEnabled(true);

		// Patterns for the TargetCountrySupplier
		// a react pattern to get from targets to countries
		GraphPattern gp3in = new GraphPattern(prefixes, "?id rdf:type v1905:Target . ?id v1905:hasName ?name .");
		GraphPattern gp3out = new GraphPattern(prefixes, "?id v1905:hasCountry ?country .");
		ReactKnowledgeInteraction reactKI = new ReactKnowledgeInteraction(new CommunicativeAct(), gp3in, gp3out,
				"reactCountry");
		kbTargetCountrySupplier.register(reactKI, (anRKI, aReactExchangeInfo) -> {

			LOG.info("TargetCountrySupplier Reacting...");
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

		kbTargetCountrySupplier.setDomainKnowledge(ruleSet);

	}

	@AfterEach
	public void cleanup() throws InterruptedException, ExecutionException {
		LOG.info("Clean up: {}", TestDynamicSemanticComposition.class.getSimpleName());
		kn.stop().get();
	}
}
