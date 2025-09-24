package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.Table;

@TestInstance(Lifecycle.PER_CLASS)
public class ForwardTest {

	private static final Logger LOG = LoggerFactory.getLogger(ForwardTest.class);

	private RuleStore store;
	private Rule optionalRule;

	private Rule grandParentRule;

	private Rule converterRule;

	private static class MyBindingSetHandler implements SinkBindingSetHandler {

		private BindingSet bs;

		@Override
		public CompletableFuture<Void> handle(BindingSet bs) {

			this.bs = bs;
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.complete((Void) null);
			return future;
		}

		public BindingSet getBindingSet() {
			return bs;
		}

	}

	@BeforeAll
	public void init() {
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isParentOf> ?y"));
		antecedent.add(new TriplePattern("?y <isParentOf> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isGrandParentOf> ?z"));
		grandParentRule = new Rule(antecedent, consequent);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
				"a", "b", "n"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>,\"Barry\"", "<janny>,<barry>,\"Janny\"", "<fenna>,<benno>,\"Fenna\"",
				"<benno>,<loes>,\"Benno\"",
				// @formatter:on
		}));

		optionalRule = new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <isParentOf> ?b"), new TriplePattern("?a <hasName> ?n"))),
				aBindingSetHandler);

		converterRule = new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInF> ?y"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInC> ?z"))), new TransformBindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Float celcius = (Float) ((Node_Literal) b.get("y")).getLiteralValue();
							bindings.append("z=" + convert(celcius) + ",x="
									+ FmtUtils.stringForNode(b.get("x"), new PrefixMappingZero()) + "|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();

						future.handle((r, e) -> {

							if (r == null) {
								LOG.error("An exception has occured on Celsius <-> Fahrenheit test", e);
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
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), aBindingSetHandler));
		store.addRule(grandParentRule);

		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), aGoal);
		store.addRule(aStartRule);

		ReasonerPlan rp = new ReasonerPlan(store, aStartRule);

		System.out.println(rp);

		store.printGraphVizCode(rp);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>", "<janny>,<barry>", "<fenna>,<benno>", "<benno>,<loes>",
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		assertEquals(
				BindingSet.fromStringData(new Table(new String[] { "x", "z" },
						new String[] { "<janny>,<fenna>", "<barry>,<benno>", "<fenna>,<loes>", }).getData()),
				aBindingSetHandler.getBindingSet());
	}

	@Test
	public void testDoNotHandleEmptyBindingSets() throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp = new TriplePattern("<barry> <isGrandParentOf> ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler() {

			@Override
			public CompletableFuture<Void> handle(BindingSet bs) {
				fail("An empty bindingset should not be handled.");

				CompletableFuture<Void> future = new CompletableFuture<>();
				future.complete((Void) null);
				return future;
			}

		};
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), aBindingSetHandler));
		store.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));
		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), aGoal);
		store.addRule(aStartRule);
		ReasonerPlan rp = new ReasonerPlan(store, aStartRule);

		System.out.println(rp);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<fenna>,<benno>", "<benno>,<loes>",
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rp.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		System.out.println("Result: " + aBindingSetHandler.getBindingSet() + " (expected null)");
		assertEquals(aBindingSetHandler.getBindingSet(), null);
	}

	@Test
	public void testMultipleLeafs() throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), aBindingSetHandler1));

		TriplePattern tp2 = new TriplePattern("?a <isGrandParentOf> ?b");
		MyBindingSetHandler aBindingSetHandler2 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp2)), aBindingSetHandler2));
		store.addRule(this.optionalRule);
		store.addRule(grandParentRule);
		Set<TriplePattern> aPremise = new HashSet<>();
		aPremise.add(new TriplePattern("?x <isParentOf> ?y"));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), aPremise);
		store.addRule(aStartRule);
		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);
		rn.setUseTaskBoard(false);
		store.printGraphVizCode(rn);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>", "<janny>,<barry>", "<fenna>,<benno>", "<benno>,<loes>",
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		assertTrue(aBindingSetHandler2.getBindingSet() != null);
		assertTrue(aBindingSetHandler1.getBindingSet() != null);

		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());
		assertFalse(aBindingSetHandler2.getBindingSet().isEmpty());

		System.out.println("Result1: " + aBindingSetHandler1.getBindingSet());
		System.out.println("Result2: " + aBindingSetHandler2.getBindingSet());
	}

	@Test
	public void testBackwardChainingDuringForwardChaining() throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp = new TriplePattern("?x <isParentOf> ?z");
		TriplePattern tp2 = new TriplePattern("?x <hasName> ?n");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp, tp2)), aBindingSetHandler));
		store.addRule(this.optionalRule);
//		store.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), aGoal);
		store.addRule(aStartRule);
		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

		System.out.println(rn);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>", "<janny>,<barry>", "<fenna>,<benno>", "<benno>,<loes>",
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		assertNotNull(aBindingSetHandler.getBindingSet());
		assertTrue(!aBindingSetHandler.getBindingSet().isEmpty());
		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
	}

	/**
	 * TODO But of course, in this scenario, the idea is that backward chaining does
	 * not happen at all, because the graph pattern is already fully covered. This
	 * part is also not implemented yet!
	 */
	@Test
	public void testNoBackwardChainingDuringForwardChainingIfFullMatch()
			throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12)), aBindingSetHandler1));

		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);
		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);

		TriplePattern tp32 = new TriplePattern("?s <type> <Sensor>");
		TriplePattern tp31 = new TriplePattern("?s <hasMeasuredValue> ?v");
		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31, tp32)),
				new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "v"
				// @formatter:on
				}, new String[] {
				// @formatter:off
						"<sens1>,4", "<sens2>,5", "<sens3>,6"
				// @formatter:on
				}))));

		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

//		store.findAllMatches();

		store.printGraphVizCode(rn);

		LOG.info("\n{}", rn);
		BindingSet bs = new BindingSet();
		bs.addAll(new Table(new String[] {
				// @formatter:off
				"sensor", "value"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<sens1>,1"
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		LOG.info("\n{}", rn);

		LOG.info("BindingSet: {}", aBindingSetHandler1.getBindingSet());
		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());

		assertEquals(1, aBindingSetHandler1.getBindingSet().size());
	}

	@Test
	public void testBackwardChainingDuringForwardChainingIfPartialMatch()
			throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		TriplePattern tp13 = new TriplePattern("?sens <isInRoom> ?room");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12, tp13)), aBindingSetHandler1));

		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);

		TriplePattern tp31 = new TriplePattern("?s <isInRoom> ?r");
		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31)),
				new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "r"
				// @formatter:on
				}, new String[] {
				// @formatter:off
						"<sens1>,<room1>", "<sens2>,<room2>", "<sens3>,<room3>"
				// @formatter:on
				}))));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);
		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

		store.printGraphVizCode(rn);

		System.out.println(rn);
		BindingSet bs = new BindingSet();
		bs.addAll(new Table(new String[] {
				// @formatter:off
				"sensor", "value"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<sens1>,1"
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertTrue(!aBindingSetHandler1.getBindingSet().isEmpty());
		assertEquals(aBindingSetHandler1.getBindingSet().size(), 1);
	}

	@Test
	public void testBackwardChainingDuringForwardChainingIfPartialWithTwoStages()
			throws InterruptedException, ExecutionException {

		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		TriplePattern tp13 = new TriplePattern("?sens <isInArea> ?area");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12, tp13)), aBindingSetHandler1));

		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);

		TriplePattern tp31 = new TriplePattern("?s <isInRoom> ?r");
		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31)),
				new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "r"
				// @formatter:on
				}, new String[] {
				// @formatter:off
						"<sens1>,<room1>", "<sens2>,<room2>", "<sens3>,<room3>"
				// @formatter:on
				}))));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);

		TriplePattern tp41 = new TriplePattern("?s <isInRoom> ?r");
		TriplePattern tp42 = new TriplePattern("?s <isInArea> ?r");
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp41)), new HashSet<>(Arrays.asList(tp42))));

		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

		store.printGraphVizCode(rn);

		System.out.println(rn);
		BindingSet bs = new BindingSet();
		bs.addAll(new Table(new String[] {
				// @formatter:off
				"sensor", "value"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<sens1>,1"
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertTrue(!aBindingSetHandler1.getBindingSet().isEmpty());
		assertEquals(aBindingSetHandler1.getBindingSet().size(), 1);
	}

	@Test
	public void testAlternativeForFullMatch() throws InterruptedException, ExecutionException {

//		-> ?s rdf:type :Sensor . ?s :hasValueInC ?v . ?s :isInArea ?b .
//
//				?s = sens1
//				?v = 21
//				?b = badkamer
//
//				-> ?x :isPartOf ?y
//
//
//				?s :isInArea ?b . ?b :isPartOf ?c -> ?s :isInArea ?c .

		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		TriplePattern tp13 = new TriplePattern("?sens <isInArea> ?area");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12, tp13)), aBindingSetHandler1));

		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		TriplePattern tp23 = new TriplePattern("?sensor <isInArea> ?area");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);
		premise.add(tp23);
		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);

		TriplePattern tp31 = new TriplePattern("?s <isPartOf> ?a");
		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31)),
				new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "a"
				// @formatter:on
				}, new String[] {
				// @formatter:off
						"<badkamer>,<bovenverdieping>", 
						"<woonkamer>,<benedenverdieping>",
						"<slaapkamer>,<bovenverdieping>"
				// @formatter:on
				}))));

		TriplePattern tp41 = new TriplePattern("?s <isInArea> ?b");
		var tp42 = new TriplePattern("?b <isPartOf> ?c");
		var tp43 = new TriplePattern("?s <isInArea> ?c");
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp41, tp42)), new HashSet<>(Arrays.asList(tp43))));

		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

		store.printGraphVizCode(rn);

		BindingSet bs = new BindingSet();
		bs.addAll(new Table(new String[] {
				// @formatter:off
				"sensor", "value", "area"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<sens1>,1,<badkamer>"
				// @formatter:on
		}).getData());

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		LOG.info("BindingSet: {}", aBindingSetHandler1.getBindingSet());
		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());

		assertEquals(2, aBindingSetHandler1.getBindingSet().size());

	}

	@Test
	public void testPublishedValuesShouldRemainAccessible() throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasValInC> ?value");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12)), aBindingSetHandler1));

		store.addRule(this.converterRule);

		TriplePattern tp31 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp32 = new TriplePattern("?sensor <hasValInF> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp31);
		premise.add(tp32);

		class StoreBindingSetHandler implements TransformBindingSetHandler {

			private BindingSet b = null;

			public StoreBindingSetHandler(BindingSet aB) {
				this.b = aB;
			}

			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {
				CompletableFuture<BindingSet> future = new CompletableFuture<>();

				future.handle((r, e) -> {

					if (r == null) {
						LOG.error("An exception has occured while testing are published values remained accessible ",
								e);
						return null;
					} else {
						return r;
					}
				});
				future.complete(this.b);
				return future;
			}
		}

		BindingSet bs = new BindingSet();
		bs.addAll(new Table(new String[] {
				// @formatter:off
				"sensor", "value"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<sens1>,\"69.0\"^^<http://www.w3.org/2001/XMLSchema#float>"
				// @formatter:on
		}).getData());
		store.addRule(
				new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31, tp32)), new StoreBindingSetHandler(bs)));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);
		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

		System.out.println(rn);

		TaskBoard tb;
		while ((tb = rn.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertNotNull(aBindingSetHandler1.getBindingSet());
		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());
	}

	/**
	 * This test checks whether combining results from two antecedent succeeds.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testCorrectlyCombineMultipleNeighbors() throws InterruptedException, ExecutionException {

		var aStore = new RuleStore();

		TriplePattern tp1 = new TriplePattern("?s1 <pred> ?o1");
		TriplePattern tp2 = new TriplePattern("?s2 <pred> ?o2");
		TriplePattern tp21 = new TriplePattern("?s2 <pred1> ?o2");
		TriplePattern tp3 = new TriplePattern("?s3 <pred> ?o3");
		TriplePattern tp31 = new TriplePattern("?s3 <pred1> ?o3");
		TriplePattern tp4 = new TriplePattern("?s4 <pred1> ?o4");

		// rule 1
		Set<TriplePattern> aConsequent = new HashSet<>();
		aConsequent.add(tp1);
		ProactiveRule r1 = new ProactiveRule(new HashSet<>(), aConsequent);
		aStore.addRule(r1);

		// rule 2
		var hashSet = new HashSet<TriplePattern>();
		hashSet.add(tp2);
		var hashSet2 = new HashSet<TriplePattern>();
		hashSet2.add(tp21);
		Rule r2 = new Rule(hashSet, hashSet2, bs -> {

			BindingSet aBS = new BindingSet();

			Binding e = new Binding("s2", "<s2>");
			e.put("o2", "<o2>");
			aBS.add(e);

			var future = new CompletableFuture<BindingSet>();
			future.complete(aBS);

			return future;

		});
		aStore.addRule(r2);

		// rule 3
		hashSet = new HashSet<TriplePattern>();
		hashSet.add(tp3);
		hashSet2 = new HashSet<TriplePattern>();
		hashSet2.add(tp31);
		Rule r3 = new Rule(hashSet, hashSet2, bs -> {

			BindingSet aBS = new BindingSet();

			Binding e = new Binding("s3", "<s3>");
			e.put("o3", "<o3>");
			aBS.add(e);

			var future = new CompletableFuture<BindingSet>();
			future.complete(aBS);

			return future;

		});
		aStore.addRule(r3);

		// rule 4
		Set<TriplePattern> anAntecedent = new HashSet<>();
		anAntecedent.add(tp4);
		MyBindingSetHandler aSinkBindingSetHandler = new MyBindingSetHandler();
		Rule r4 = new Rule(anAntecedent, aSinkBindingSetHandler);
		aStore.addRule(r4);

		ReasonerPlan rp = new ReasonerPlan(aStore, r1);

		aStore.printGraphVizCode(rp);

		BindingSet bindingSet = new BindingSet();
		Binding e = new Binding("s1", "<s1>");
		e.put("o1", "<o1>");
		bindingSet.add(e);

		TaskBoard tb;
		while ((tb = rp.execute(bindingSet)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet bindingSet2 = aSinkBindingSetHandler.getBindingSet();
		System.out.println(bindingSet2);

		Binding e1 = new Binding("o4", "<o3>");
		e1.put("s4", "<s3>");

		Binding e2 = new Binding("s4", "<s2>");
		e2.put("o4", "<o2>");

		assertFalse(bindingSet2.isEmpty());
		assertTrue(bindingSet2.contains(e1));
		assertTrue(bindingSet2.contains(e2));

	}
}