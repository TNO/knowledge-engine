package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDFloat;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.util.FmtUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class BackwardTest {

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

	private static final Logger LOG = LoggerFactory.getLogger(BackwardTest.class);

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

		// Start reasoning
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, requestNonExistingDataRule, taskboard);

		store.printGraphVizCode(root);

		System.out.println(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "21");
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();
		System.out.println("bindings: " + bind);
		assertTrue(isEmpty(bind));

	}

	private boolean isEmpty(BindingSet b) {
		if (b.isEmpty() || b.iterator().next().isEmpty())
			return true;

		return false;
	}

	@Test
	public void testConverter() throws InterruptedException, ExecutionException {
		TaskBoard taskboard = new TaskBoard();
		ReasonerPlan root = new ReasonerPlan(store, converterRule, taskboard);

		store.printGraphVizCode(root);

		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testMoreThanOneInputBinding() throws InterruptedException, ExecutionException {

		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, moreThanOneInputBindingRule, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertFalse(isEmpty(bind));

	}

	@Test
	public void testMoreThanOneInputBinding2() throws InterruptedException, ExecutionException {

		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, moreThanOneInputBinding2Rule, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testTwoPropsToAndFromTheSameVars() throws InterruptedException, ExecutionException {

		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, twoPropsToAndFromTheSameVarsRule, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
//		Binding binding = new Binding();
//		binding.put("q", "22");
//		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>");
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());

		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertTrue(isEmpty(bind));
	}

	@Test
	public void testVariableMatchesLiteralInGraphPattern() throws InterruptedException, ExecutionException {
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, variableMatchesLiteralInGraphPatternRule, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("p", "<sensor1>");
		binding2.put("q", "\"22.0\"^^<http://www.w3.org/2001/XMLSchema#float>");
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}

	@Test
	public void testVariableAsPredicate() throws InterruptedException, ExecutionException {
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, variableAsPredicateRule, taskboard);

		store.printGraphVizCode(root);

		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}

	@Test
	public void testVariableAsPredicate2() throws InterruptedException, ExecutionException {
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, variableAsPredicate2Rule, taskboard);
		store.printGraphVizCode(root);

		// empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty()); // TODO THIS ONE SHOULD CONTAIN ONLY sensor1
	}

	@Test
	public void testAllTriples() throws InterruptedException, ExecutionException {
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan root = new ReasonerPlan(store, allTriplesRule, taskboard);
		System.out.println(root);

		// empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		do {
			taskboard.executeScheduledTasks().get();
			root.execute(bs);
		} while (taskboard != null && !taskboard.tasks.isEmpty());
		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}
}
