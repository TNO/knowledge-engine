package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.api.TripleNode;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.TripleVarBinding;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.smartconnector.impl.Util;
import eu.knowledge.engine.smartconnector.util.KnowledgeNetwork;
import eu.knowledge.engine.smartconnector.util.KnowledgeBaseImpl;

public class TestComplexGraphPatternMatching {

	private static final Logger LOG = LoggerFactory.getLogger(TestComplexGraphPatternMatching.class);

	private static KnowledgeBaseImpl devicesKB;
	private static KnowledgeBaseImpl dashboardKB;
	private static KnowledgeBaseImpl observationsKB;

	@BeforeAll
	public static void setup() throws InterruptedException, BrokenBarrierException, TimeoutException {
	}

	@Test
	public void testComplexGraphPattern() throws InterruptedException {

		PrefixMappingMem prefixes = new PrefixMappingMem();
		prefixes.setNsPrefixes(PrefixMapping.Standard);
		prefixes.setNsPrefix("ex", "https://www.tno.nl/example/");
		prefixes.setNsPrefix("saref", "https://saref.etsi.org/core/");

		var kn = new KnowledgeNetwork();

		createDevicesKB(prefixes, kn);
		createObservationsKB(prefixes, kn);
		AskKnowledgeInteraction askKI = createDashboardKB(prefixes, kn);
		LOG.info("Waiting until everyone is up-to-date!");
		kn.sync();
		LOG.info("Everyone is up-to-date!");

		// start testing!
		BindingSet bindings = null;
		AskResult result = null;
		try {
			LOG.trace("Before ask.");

			AskResult askResult = dashboardKB.ask(askKI, new RecipientSelector(), new BindingSet()).get();

			LOG.info("Gaps: {}", askResult.getKnowledgeGaps());

			askResult.getReasonerPlan().getStore().printGraphVizCode(askResult.getReasonerPlan());

			bindings = askResult.getBindings();
			LOG.trace("After ask.");

			Set<URI> kbIds = askResult.getExchangeInfoPerKnowledgeBase().stream()
					.map(AskExchangeInfo::getKnowledgeBaseId).collect(Collectors.toSet());

			assertEquals(
					new HashSet<URI>(
							Arrays.asList(devicesKB.getKnowledgeBaseId(), observationsKB.getKnowledgeBaseId())),
					kbIds, "The result should come from observationsKB and devicesKB not: " + kbIds);

			LOG.info("Bindings: {}", bindings);
			assertEquals(1, bindings.size());

		} catch (InterruptedException | ExecutionException e) {
			fail();
		}
	}

	public void findMissingVars(Set<TriplePattern> gp, TripleVarBinding tvb) {

		for (TriplePattern tp : gp) {

			Node subjectN = null;
			if (tp.getSubject() instanceof Var) {
				subjectN = tvb.get(new TripleNode(tp, tp.getSubject(), 0));
				if (subjectN == null) {
					LOG.info("{}", tp);
					continue;
				}
			}

			Node predicateN = null;
			if (tp.getPredicate() instanceof Var) {
				predicateN = tvb.get(new TripleNode(tp, tp.getPredicate(), 1));
				if (predicateN == null) {
					LOG.info("{}", tp);
					continue;
				}
			}

			Node objectN = null;
			if (tp.getObject() instanceof Var) {
				objectN = tvb.get(new TripleNode(tp, tp.getObject(), 2));
				if (objectN == null) {
					LOG.info("{}", tp);
					continue;
				}
			}
		}

	}

	private AskKnowledgeInteraction createDashboardKB(PrefixMappingMem prefixes, KnowledgeNetwork kn) {

		dashboardKB = new KnowledgeBaseImpl("dashboardKB");
		dashboardKB.setReasonerEnabled(true);
		kn.addKB(dashboardKB);

		String gp = """
				?x ex:locatedIn ?y .
				?y ex:locatedIn ?z .
				""";
		Set<TriplePattern> antecedent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		gp = """
				?x ex:locatedIn ?z .
				""";
		Set<TriplePattern> consequent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		Rule locatedInRule = new Rule(antecedent, consequent);

		gp = """
				?p rdf:type saref:Thermostat .
				""";
		antecedent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		gp = """
				?p rdf:type saref:Sensor .
				""";
		consequent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		Rule thermostatSensorRule = new Rule(antecedent, consequent);

		gp = """
				?p rdf:type saref:OpenCloseSensor .
				""";
		antecedent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		gp = """
				?p rdf:type saref:Sensor .
				""";
		consequent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		Rule opencloseSensorRule = new Rule(antecedent, consequent);

		gp = """
				?r saref:hasPropertyValue ?p1 .
				?p1 rdf:type saref:PropertyValue .
				?p1 saref:isMeasuredIn <https://qudt.org/2.1/vocab/unit/DEG_F> .
				?p1 saref:hasValue ?v1 .
				""";
		antecedent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		gp = """
				?r saref:hasPropertyValue ?p2 .
				?p2 rdf:type saref:PropertyValue .
				?p2 saref:isMeasuredIn <https://qudt.org/2.1/vocab/unit/DEG_C> .
				?p2 saref:hasValue ?v2 .
				""";
		consequent = new HashSet<>(TestUtils.toGP(prefixes, gp));
		Rule fahrenheitConverterRule = new Rule(antecedent, consequent, new TransformBindingSetHandler() {

			@Override
			public CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> handle(
					eu.knowledge.engine.reasoner.api.BindingSet bs) {
				eu.knowledge.engine.reasoner.api.BindingSet bindingSet = new eu.knowledge.engine.reasoner.api.BindingSet();
				eu.knowledge.engine.reasoner.api.Binding binding = new eu.knowledge.engine.reasoner.api.Binding();
				for (eu.knowledge.engine.reasoner.api.Binding b : bs) {
					Object fahrenheitObj = b.get("v1").getLiteralValue();
					Float fahrenheit = null;
					if (fahrenheitObj instanceof BigDecimal) {
						fahrenheit = (Float) ((BigDecimal) fahrenheitObj).floatValue();
					} else if (fahrenheitObj instanceof Integer) {
						fahrenheit = (float) ((int) fahrenheitObj);
					} else {
						fahrenheit = (Float) fahrenheitObj;
					}
					LOG.info("Converting fahrenheit '{}' to celcius.", fahrenheit);
					binding.put("r", FmtUtils.stringForNode(b.get("r"), new PrefixMappingZero()));
					binding.put("v2", "\"" + convert(fahrenheit) + "\"^^<http://www.w3.org/2001/XMLSchema#float>");
					binding.put("p2",
							FmtUtils.stringForNode(b.get("p1"), new PrefixMappingZero()).replace(">", "_new>"));
					bindingSet.add(binding);
				}

				CompletableFuture<eu.knowledge.engine.reasoner.api.BindingSet> future = new CompletableFuture<>();

				future.handle((r, e) -> {

					if (r == null) {
						LOG.error("An exception has occured in Celsius <-> Fahrenheit test", e);
						return null;
					} else {
						return r;
					}
				});
				future.complete(bindingSet);
				return future;
			}

			public float convert(float fahrenheit) {
				return ((fahrenheit - 32) * 5) / 9;
			}

		});

		String pattern = """
				?device rdf:type saref:Sensor .
				?device ex:releaseDate ?releaseDate .
				?device ex:owner ?owner .
				?device ex:purchasePrice ?price .
				?device ex:latitude ?lat .
				?device ex:longitude ?lon .
				?device ex:height ?height .
				?device ex:locatedIn <http://building> .
				?device ex:manufacturer ?manufacturer.
				?device rdfs:label ?label .
				?device rdfs:comment ?comment .
				?device ex:propA ?propA .
				?device ex:propB ?propB .
				?device ex:propC ?propC .
				?device ex:propD ?propD .
				?device ex:propE ?propE .
				?device ex:propF ?propF .
				?device ex:propG ?propG .
				?device ex:propH ?propH .
				?obs rdf:type saref:Observation .
				?obs saref:madeBy ?device .
				?obs saref:hasTimestamp ?timestamp .
				?obs saref:hasResult ?result .
				?result saref:hasPropertyValue ?propVal .
				?propVal rdf:type saref:PropertyValue .
				?propVal saref:hasValue ?val .
				?propVal saref:isMeasuredIn <https://qudt.org/2.1/vocab/unit/DEG_C> .

				""";

		GraphPattern gp2 = new GraphPattern(prefixes, TestUtils.convertGP(pattern));
		AskKnowledgeInteraction askKI = new AskKnowledgeInteraction(new CommunicativeAct(), gp2, null, false, false,
				true, MatchStrategy.NORMAL_LEVEL);
		dashboardKB.setDomainKnowledge(new HashSet<>(
				Arrays.asList(locatedInRule, thermostatSensorRule, opencloseSensorRule, fahrenheitConverterRule)));
		dashboardKB.register(askKI);
		return askKI;
	}

	private void createDevicesKB(PrefixMappingMem prefixes, KnowledgeNetwork kn) {
		devicesKB = new KnowledgeBaseImpl("devicesKB");
		devicesKB.setReasonerEnabled(true);
		kn.addKB(devicesKB);
		GraphPattern gp1 = new GraphPattern(prefixes, TestUtils.convertGP("""
				?d rdf:type ?type .
				?d rdfs:label ?l .
				?d rdfs:comment ?c .
				?d ex:manufacturer ?m .
				?d ex:locatedIn ?b .
				?d ex:releaseDate ?r .
				?d ex:owner ?o .
				?d ex:purchasePrice ?p .
				?d ex:latitude ?lat .
				?d ex:longitude ?lon .
				?d ex:height ?h .
				?d ex:propA ?propA .
				?d ex:propB ?propB .
				?d ex:propC ?propC .
				?d ex:propD ?propD .
				?d ex:propE ?propE .
				?d ex:propF ?propF .
				?d ex:propG ?propG .
				?d ex:propH ?propH .
				"""));
		AnswerKnowledgeInteraction aKI = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp1);
		devicesKB.register(aKI, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();

			binding.put("type", "<https://saref.etsi.org/core/Thermostat>");
			binding.put("d", "<thermostat1>");
			binding.put("l", "\"Thermostat 1\"");
			binding.put("c", "\"The thermostat in the bathroom.\"");
			binding.put("m", "\"Philips\"");
			binding.put("b", "<http://bathroom>");
			binding.put("r", "\"2018-04-09T10:00:00Z\"^^xsd:dateTime");
			binding.put("o", "\"Jan Treurning\"");
			binding.put("p", "\"EUR432\"");
			binding.put("lat", "\"52.5939466\"");
			binding.put("lon", "\"6.3671058\"");
			binding.put("h", "\"234cm\"");
			binding.put("propA", "<propA>");
			binding.put("propB", "<propB>");
			binding.put("propC", "<propC>");
			binding.put("propD", "<propD>");
			binding.put("propE", "<propE>");
			binding.put("propF", "<propF>");
			binding.put("propG", "<propG>");
			binding.put("propH", "<propH>");
			bindingSet.add(binding);

			binding = new Binding();
			binding.put("type", "<https://www.tno.nl/example/OpenCloseSensor>");
			binding.put("d", "<openclose1>");
			binding.put("l", "\"Open Close Sensor 1\"");
			binding.put("c", "\"The open close sensor of the front door.\"");
			binding.put("m", "\"Monnit\"");
			binding.put("b", "<http://hallway>");
			binding.put("r", "\"2020-01-02T21:35:00Z\"^^xsd:dateTime");
			binding.put("o", "\"Jan Treurning\"");
			binding.put("p", "\"EUR1481.55\"");
			binding.put("lat", "\"52.5939466\"");
			binding.put("lon", "\"6.3671058\"");
			binding.put("h", "\"104cm\"");
			binding.put("propA", "<propA>");
			binding.put("propB", "<propB>");
			binding.put("propC", "<propC>");
			binding.put("propD", "<propD>");
			binding.put("propE", "<propE>");
			binding.put("propF", "<propF>");
			binding.put("propG", "<propG>");
			binding.put("propH", "<propH>");
			bindingSet.add(binding);

			Util.removeRedundantBindingsAnswer(anAnswerExchangeInfo.getIncomingBindings(), bindingSet);

			return bindingSet;
		});

		GraphPattern gp2 = new GraphPattern(prefixes, TestUtils.convertGP("""
				?x ex:locatedIn ?y .
				"""));
		AnswerKnowledgeInteraction aKI2 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp2);
		devicesKB.register(aKI2, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {

			BindingSet bindingSet = new BindingSet();
			Binding binding = new Binding();
			binding.put("x", "<http://bathroom>");
			binding.put("y", "<http://first_floor>");
			bindingSet.add(binding);

			binding = new Binding();
			binding.put("x", "<http://hallway>");
			binding.put("y", "<http://round_floor>");
			bindingSet.add(binding);

			binding = new Binding();
			binding.put("x", "<http://ground_floor>");
			binding.put("y", "<http://building>");
			bindingSet.add(binding);

			binding = new Binding();
			binding.put("x", "<http://first_floor>");
			binding.put("y", "<http://building>");
			bindingSet.add(binding);

			Util.removeRedundantBindingsAnswer(anAnswerExchangeInfo.getIncomingBindings(), bindingSet);

			return bindingSet;
		});
	}

	private void createObservationsKB(PrefixMappingMem prefixes, KnowledgeNetwork kn) {
		observationsKB = new KnowledgeBaseImpl("observationsKB");
		observationsKB.setReasonerEnabled(true);
		kn.addKB(observationsKB);

		GraphPattern gp12 = new GraphPattern(prefixes, TestUtils.convertGP("""
				?obs rdf:type saref:Observation .
				?obs saref:madeBy ?device .
				?obs saref:hasTimestamp ?timestamp .
				?obs saref:hasResult ?result .
				?result saref:hasPropertyValue ?propVal .
				?propVal rdf:type saref:PropertyValue .
				?propVal saref:hasValue ?val .
				?propVal saref:isMeasuredIn ?prop .
								"""));
		AnswerKnowledgeInteraction aKI12 = new AnswerKnowledgeInteraction(new CommunicativeAct(), gp12);
		observationsKB.register(aKI12, (AnswerHandler) (anAKI, anAnswerExchangeInfo) -> {

			BindingSet bindingSet = new BindingSet();

			Binding binding = new Binding();
			binding.put("obs", "<openclose1_observation1>");
			binding.put("device", "<openclose1>");
			binding.put("timestamp", "\"2024-08-013T09:30:00Z\"^^xsd:dateTime");
			binding.put("result", "<result_openclose1_1>");
			binding.put("propVal", "<openclose1_propval1>");
			binding.put("val", "<https://saref.etsi.org/core/Open>");
			binding.put("prop", "<https://saref.etsi.org/core/OpenClose>");
			bindingSet.add(binding);

			binding = new Binding();
			binding.put("obs", "<thermostat1_observation1>");
			binding.put("device", "<thermostat1>");
			binding.put("timestamp", "\"2024-08-013T09:45:00Z\"^^xsd:dateTime");
			binding.put("result", "<result_thermostat1_1>");
			binding.put("propVal", "<thermostat1_propval1>");
			binding.put("val", "\"69.0\"^^<http://www.w3.org/2001/XMLSchema#float>");
			binding.put("prop", "<https://qudt.org/2.1/vocab/unit/DEG_F>");
			bindingSet.add(binding);
			Util.removeRedundantBindingsAnswer(anAnswerExchangeInfo.getIncomingBindings(), bindingSet);
			return bindingSet;
		});
	}

	@AfterAll
	public static void cleanup() {
		LOG.info("Clean up: {}", TestComplexGraphPatternMatching.class.getSimpleName());
		if (devicesKB != null) {
			devicesKB.stop();
		} else {
			fail("KB1 should not be null!");
		}

		if (dashboardKB != null) {

			dashboardKB.stop();
		} else {
			fail("KB2 should not be null!");
		}

		if (observationsKB != null) {
			observationsKB.stop();
		} else {
			fail("KB3 should not be null!");
		}

	}
}
