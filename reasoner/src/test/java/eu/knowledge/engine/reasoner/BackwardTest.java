package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.Table;

@TestInstance(Lifecycle.PER_CLASS)
public class BackwardTest {
	private static final Logger LOG = LoggerFactory.getLogger(BackwardTest.class);
	private RuleStore store;

	private ProactiveRule requestNonExistingDataRule;
	private ProactiveRule converterRule;
	private ProactiveRule moreThanOneInputBindingRule;
	private ProactiveRule moreThanOneInputBinding2Rule;
	private ProactiveRule twoPropsToAndFromTheSameVarsRule;
	private ProactiveRule variableMatchesLiteralInGraphPatternRule;
	private ProactiveRule variableAsPredicateRule;
	private ProactiveRule variableAsPredicate2Rule;
	private ProactiveRule allTriplesRule;

	@BeforeAll
	public void init() throws URISyntaxException {
		// Initialize
		store = new RuleStore();
		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"a", "b"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor2>,\"21.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?e <type> <Sensor>"), new TriplePattern("?e <hasValInF> ?f"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"e", "f"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor3>,\"69.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor4>,\"71.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?k <type> <Sensor>"), new TriplePattern("?k <hasValInK> ?w"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"k", "w"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor5>,\"295.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor6>,\"294.0\"^^<http://www.w3.org/2001/XMLSchema#float>"
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Sensor>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Device>")))));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInF> ?y"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInC> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Object kelvinObj = b.get("y").getLiteralValue();
							Float celsius = null;
							if (kelvinObj instanceof BigDecimal) {
								celsius = (Float) ((BigDecimal) kelvinObj).floatValue();
							} else if (kelvinObj instanceof Integer) {
								celsius = (float) ((int) kelvinObj);
							} else {
								celsius = (Float) kelvinObj;
							}
							bindings.append("z=" + "\"" + convert(celsius)
									+ "\"^^<http://www.w3.org/2001/XMLSchema#float>" + ",x="
									+ FmtUtils.stringForNode(b.get("x"), new PrefixMappingZero()) + "|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

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

				}));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInK> ?i"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInC> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {

							b.get("i").getLiteralLexicalForm();

							Object kelvinObj = b.get("i").getLiteralValue();
							Float kelvin = null;
							if (kelvinObj instanceof BigDecimal) {
								kelvin = (Float) ((BigDecimal) kelvinObj).floatValue();
							} else {
								kelvin = (Float) kelvinObj;
							}
							bindings.append("z=")
									.append("\"" + convert(kelvin) + "\"^^<http://www.w3.org/2001/XMLSchema#float>")
									.append(",x=").append(FmtUtils.stringForNode(b.get("u"), new PrefixMappingZero()))
									.append("|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured in Celsius <-> Kelvin test", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(bindingSet);
						return future;
					}

					public float convert(float kelvin) {
						return kelvin - 273;
					}
				}));

		store.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInK> ?i"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?u <hasValInF> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {

						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {

							Object kelvinObj = b.get("i").getLiteralValue();
							Float kelvin = null;
							if (kelvinObj instanceof BigDecimal) {
								kelvin = (Float) ((BigDecimal) kelvinObj).floatValue();
							} else {
								kelvin = (Float) kelvinObj;
							}

							bindings.append("z=")
									.append("\"" + convertK(kelvin) + "\"^^<http://www.w3.org/2001/XMLSchema#float>")
									.append(",u=").append(FmtUtils.stringForNode(b.get("u"), new PrefixMappingZero()))
									.append("|");
						}

						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured in Fahrenheit <-> Kelvin test", e);
								return null;
							} else {
								return r;
							}
						});
						future.complete(bindingSet);
						return future;

					}

					public float convertK(float kelvin) {
						return ((kelvin * 9) / 5) - 459;
					}
				}));

		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?i <type> <Sensor>"), new TriplePattern("?i <inRoom> ?j"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"i", "j"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor3>,<kitchen>",
						"<sensor4>,<livingroom>",
						"<sensor1>,<bathroom>",
						"<sensor2>,<bedroom>",
						//@formatter:on
				}))));

		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Device>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		requestNonExistingDataRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(requestNonExistingDataRule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		converterRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(converterRule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Device>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		// objective.add(new TriplePattern("?p hasValInT ?q")); //TODO this still does
		// not work
		moreThanOneInputBindingRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(moreThanOneInputBindingRule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		moreThanOneInputBinding2Rule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(moreThanOneInputBinding2Rule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Device>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <nonExistentProp> ?q"));
		twoPropsToAndFromTheSameVarsRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(twoPropsToAndFromTheSameVarsRule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> ?t"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		variableMatchesLiteralInGraphPatternRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(variableMatchesLiteralInGraphPatternRule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p ?pred \"21.666666\"^^<http://www.w3.org/2001/XMLSchema#float>"));
//		objective.add(new TriplePattern("?p ?pred 22"));
		variableAsPredicateRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(variableAsPredicateRule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p ?pred \"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>"));
		variableAsPredicate2Rule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(variableAsPredicate2Rule);

		// Formulate objective
		objective = new HashSet<>();
		objective.add(new TriplePattern("?s ?p ?o"));
		allTriplesRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(allTriplesRule);
	}

	@Test
	public void testRequestNonExistingData() throws InterruptedException, ExecutionException {

		RuleStore aStore = new RuleStore();
		aStore.addRule(requestNonExistingDataRule);
		aStore.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Sensor>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Device>")))));
		aStore.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"a", "b"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor2>,\"21.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(aStore, requestNonExistingDataRule);

		System.out.println(root);

		aStore.printGraphVizCode(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "21");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet bind = root.getResults();
		System.out.println("bindings: " + bind);
		assertTrue(isEmpty(bind));

	}

	private boolean isEmpty(BindingSet b) {
		if (b.isEmpty() || b.iterator().next().isEmpty())
			return true;

		return false;
	}

	// TODO: Add more detailed assertions: What do I expect here?
	@Test
	public void testConverter() throws InterruptedException, ExecutionException {
		// Formulate objective
		HashSet<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		ProactiveRule converterRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(converterRule);

		ReasonerPlan plan = new ReasonerPlan(store, converterRule);

		store.printGraphVizCode(plan);

		TaskBoard tb;
		while ((tb = plan.execute(new BindingSet(new Binding()))).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet result = plan.getResults();

		System.out.println("bindings: " + result);
		assertFalse(result.isEmpty());

	}

	@Test
	public void testMoreThanOneInputBinding() throws InterruptedException, ExecutionException {

		ReasonerPlan root = new ReasonerPlan(store, moreThanOneInputBindingRule);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();

		System.out.println("bindings: " + bind);
		assertFalse(isEmpty(bind));

	}

	@Test
	public void testMoreThanOneInputBinding2() throws InterruptedException, ExecutionException {

		ReasonerPlan root = new ReasonerPlan(store, moreThanOneInputBinding2Rule);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testTwoPropsToAndFromTheSameVars() throws InterruptedException, ExecutionException {

		ReasonerPlan root = new ReasonerPlan(store, twoPropsToAndFromTheSameVarsRule);
		System.out.println(root);

		BindingSet bs = new BindingSet();
//		Binding binding = new Binding();
//		binding.put("q", "22");
//		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();

		System.out.println("bindings: " + bind);
		assertTrue(isEmpty(bind));
	}

	@Test
	public void testVariableMatchesLiteralInGraphPattern() throws InterruptedException, ExecutionException {

		RuleStore s = new RuleStore();
		s.addRule(variableMatchesLiteralInGraphPatternRule);
		s.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"a", "b"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor2>,\"21.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));
		s.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Sensor>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <Device>")))));

		ReasonerPlan root = new ReasonerPlan(s, variableMatchesLiteralInGraphPatternRule);

		s.printGraphVizCode(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}

	@Test
	public void testVariableAsPredicate() throws InterruptedException, ExecutionException {
		ReasonerPlan root = new ReasonerPlan(store, variableAsPredicateRule);

		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}

	@Test
	public void testVariableAsPredicate2() throws InterruptedException, ExecutionException {

		ReasonerPlan root = new ReasonerPlan(store, variableAsPredicate2Rule);

		// empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();

		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty()); // TODO THIS ONE SHOULD CONTAIN ONLY sensor1
	}

	@Test
	public void testAllTriples() throws InterruptedException, ExecutionException {
		ReasonerPlan root = new ReasonerPlan(store, allTriplesRule);
		System.out.println(root);

		// empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		BindingSet bind = root.getResults();

		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}

	/**
	 * Based on:
	 * https://github.com/apache/jena/blob/main/jena-core/src/test/java/org/apache/jena/reasoner/rulesys/test/TestFBRules.java#L139
	 * 
	 * TestFBRules#testRuleMatcher()
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	@Test
	public void jenaForwardTest() throws InterruptedException, ExecutionException, ParseException {
		RuleStore s = new RuleStore();
		var tp1 = new TriplePattern("?a <p> ?b");
		var tp2 = new TriplePattern("?b <q> ?c");
		var tp3 = new TriplePattern("?a <q> ?c");
		var r1 = new Rule(new HashSet<>(Arrays.asList(tp1, tp2)), new HashSet<>(Arrays.asList(tp3)));
		s.addRule(r1);

		var tp4 = new TriplePattern("?a <p> ?b");
		var tp5 = new TriplePattern("?b <p> ?c");
		var tp6 = new TriplePattern("?a <p> ?c");
		var r2 = new Rule(new HashSet<>(Arrays.asList(tp4, tp5)), new HashSet<>(Arrays.asList(tp6)));
		s.addRule(r2);

		var tp7 = new TriplePattern("?a <p> ?a");
		var tp8 = new TriplePattern("<n1> <p> ?c");
		var tp9 = new TriplePattern("<n1> <p> ?a");
		var tp10 = new TriplePattern("?a <p> ?c");
		var r3 = new Rule(new HashSet<>(Arrays.asList(tp7, tp8, tp9)), new HashSet<>(Arrays.asList(tp10)));
		s.addRule(r3);

		var tp11 = new TriplePattern("<n4> ?p ?a");
		var tp12 = new TriplePattern("<n4> ?a ?p");
		var r4 = new Rule(new HashSet<>(Arrays.asList(tp11)), new HashSet<>(Arrays.asList(tp12)));
		s.addRule(r4);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "p", "o"
						// @formatter:on
		}, new String[] {
				// @formatter:off
						"<n1>,<p>,<n2>", 
						"<n2>,<p>,<n3>", 
						"<n2>,<q>,<n3>",
						"<n4>,<p>,<n4>",
						// @formatter:on
		}));

		Rule r5 = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				aBindingSetHandler);
		s.addRule(r5);

		TriplePattern tp13 = new TriplePattern("?s ?p ?o");
		ProactiveRule startRule = new ProactiveRule(new HashSet<>(Arrays.asList(tp13)), new HashSet<>());
		s.addRule(startRule);

		ReasonerPlan rp = new ReasonerPlan(s, startRule);

		s.printGraphVizCode(rp);

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		bs.add(b);

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		Model m = Util.generateModel(tp13, rp.getResults());

		StmtIterator iter = m.listStatements();

		while (iter.hasNext()) {
			Statement st = iter.next();
			System.out.println(st);
		}
		assertEquals(7, rp.getResults().size()); // TODO make this assert more specific

	}

	/**
	 * Based on:
	 * https://github.com/apache/jena/blob/main/jena-core/testing/reasoners/bugs/groundClosure2.rules
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws ParseException
	 */
	@Test
	public void jenaForwardTest2() throws InterruptedException, ExecutionException, ParseException {

//		-> (eg:Paul eg:fatherTwo eg:Phil) .
//		-> (eg:Paul eg:altFather eg:Phil) .
//		-> (eg:Paul eg:builtinFather eg:Phil) .
//
//		[fatherparent: (?a eg:parent ?b) <- (?a eg:father ?b)]
//		[fatherparenttwo: (?a eg:father ?b) <- (?a eg:fatherTwo ?b)]
//		[builtinparent: (?a eg:parent ?b)<- flag() (?a eg:builtinFater ?b) ]

		RuleStore s = new RuleStore();
		var tp1 = new TriplePattern("?a <father> ?b");
		var tp2 = new TriplePattern("?a <parent> ?b");
		var r1 = new Rule(new HashSet<>(Arrays.asList(tp1)), new HashSet<>(Arrays.asList(tp2)));
		s.addRule(r1);

		var tp4 = new TriplePattern("?a <fatherTwo> ?b");
		var tp5 = new TriplePattern("?a <father> ?b");
		var r2 = new Rule(new HashSet<>(Arrays.asList(tp4)), new HashSet<>(Arrays.asList(tp5)));
		s.addRule(r2);

		var tp7 = new TriplePattern("?a <builtinFather> ?b");
		var tp8 = new TriplePattern("?a <parent> ?b");
		var r3 = new Rule(new HashSet<>(Arrays.asList(tp7)), new HashSet<>(Arrays.asList(tp8)));
		s.addRule(r3);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "p", "o"
						// @formatter:on
		}, new String[] {
				// @formatter:off
						"<paul>,<fatherTwo>,<phil>", 
						"<paul>,<altFather>,<phil>", 
						"<paul>,<builtinFather>,<phil>",
						// @formatter:on
		}));

		Rule r5 = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s ?p ?o"))),
				aBindingSetHandler);
		s.addRule(r5);

		TriplePattern tp13 = new TriplePattern("?s ?p ?o");
		ProactiveRule startRule = new ProactiveRule(new HashSet<>(Arrays.asList(tp13)), new HashSet<>());
		s.addRule(startRule);

		ReasonerPlan rp = new ReasonerPlan(s, startRule);

		s.printGraphVizCode(rp);

		BindingSet bs = new BindingSet();
		Binding b = new Binding();
		bs.add(b);

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		Model m = Util.generateModel(tp13, rp.getResults());

		StmtIterator iter = m.listStatements();

		while (iter.hasNext()) {
			Statement st = iter.next();
			System.out.println(st);
		}
		assertEquals(5, rp.getResults().size()); // TODO make this assert more specific

	}

}
