package eu.knowledge.engine.reasoner2;

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

import eu.knowledge.engine.reasoner.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.TransformBindingSetHandler;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

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
						"<barry>,<fenna>,\"Barry\"", 
						"<janny>,<barry>,\"Janny\"", 
						"<fenna>,<benno>,\"Fenna\"",
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

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>", 
				"<janny>,<barry>", 
				"<fenna>,<benno>", 
				"<benno>,<loes>",
				// @formatter:on
		}).getData());

		rp.execute(bs);

		assertEquals(
				BindingSet.fromStringData(new Table(new String[] { "x", "z" },
						new String[] { "<janny>,<fenna>", "<barry>,<benno>", "<fenna>,<loes>", }).getData()),
				aBindingSetHandler.getBindingSet());
	}

	/**
	 * TODO this is clear, empty result bindingsets should not be send to the
	 * handlers.
	 */
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
				"<fenna>,<benno>", 
				"<benno>,<loes>",
				// @formatter:on
		}).getData());

		rp.execute(bs);

		System.out.println("Result: " + aBindingSetHandler.getBindingSet() + " (expected null)");
		assertEquals(aBindingSetHandler.getBindingSet(), null);
	}

	/**
	 * TODO Probably the same problem as the backward during forward tests.
	 */
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

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				// @formatter:off
				"x", "y"
				// @formatter:on
		}, new String[] {
				// @formatter:off
				"<barry>,<fenna>", 
				"<janny>,<barry>", 
				"<fenna>,<benno>", 
				"<benno>,<loes>",
				// @formatter:on
		}).getData());

		rn.execute(bs);

		assertTrue(aBindingSetHandler2.getBindingSet() != null);
		assertTrue(aBindingSetHandler1.getBindingSet() != null);

		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());
		assertFalse(aBindingSetHandler2.getBindingSet().isEmpty());

		System.out.println("Result1: " + aBindingSetHandler1.getBindingSet());
		System.out.println("Result2: " + aBindingSetHandler2.getBindingSet());
	}

	/**
	 * TODO probably the same error as the backward during forward tests.
	 */
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
				"<barry>,<fenna>", 
				"<janny>,<barry>", 
				"<fenna>,<benno>",
				"<benno>,<loes>",
				// @formatter:on
		}).getData());

		rn.execute(bs);

		assertNotNull(aBindingSetHandler.getBindingSet());
		assertTrue(!aBindingSetHandler.getBindingSet().isEmpty());
		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
	}

	/**
	 * TODO This test does not return anything (which is wrong) and this seems to be
	 * caused by the result bindingset not being translated into a filter bindingset
	 * when going backward. But of course, in this scenario, the idea is that
	 * backward chaining does not happen at all, because the graph pattern is
	 * already fully covered. This part is also not implemented yet!
	 */
	@Test
	public void testNoBackwardChainingDuringForwardChainingIfFullMatch()
			throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12)), aBindingSetHandler1));
		store.addRule(grandParentRule);
		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);

		TriplePattern tp31 = new TriplePattern("?s <hasMeasuredValue> ?v");
		TriplePattern tp32 = new TriplePattern("?s <type> <Sensor>");
		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31, tp32)),
				new DataBindingSetHandler(new Table(new String[] {
				// @formatter:off
						"s", "v"
						// @formatter:on
				}, new String[] {
				// @formatter:off
						"<sens1>,4", 
						"<sens2>,5", 
						"<sens3>,6"
						// @formatter:on
				}))));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);
		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);

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

		rn.execute(bs);

		LOG.info("\n{}", rn);

		LOG.info("BindingSet: {}", aBindingSetHandler1.getBindingSet());
		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());

		assertEquals(1, aBindingSetHandler1.getBindingSet().size());
	}

	/**
	 * TODO It does not collect any bindingset because not all of its neighbors have
	 * contributed, but one neighbor will never contribute because it never gets a
	 * filter bindingset.
	 */
	@Test
	public void testBackwardChainingDuringForwardChainingIfPartialMatch()
			throws InterruptedException, ExecutionException {
		store = new RuleStore();
		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		TriplePattern tp13 = new TriplePattern("?sens <isInRoom> ?room");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12, tp13)), aBindingSetHandler1));
		store.addRule(grandParentRule);
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
						"<sens1>,<room1>",
						"<sens2>,<room2>", 
						"<sens3>,<room3>"
						// @formatter:on
				}))));

		ProactiveRule aStartRule = new ProactiveRule(new HashSet<>(), premise);
		store.addRule(aStartRule);
		ReasonerPlan rn = new ReasonerPlan(store, aStartRule);
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

		rn.execute(bs);

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertTrue(!aBindingSetHandler1.getBindingSet().isEmpty());
		assertEquals(aBindingSetHandler1.getBindingSet().size(), 1);
	}

	/**
	 * TODO probably the same error as the backward during forward tests.
	 */
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

		rn.execute(bs);

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertNotNull(aBindingSetHandler1.getBindingSet());
		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());
	}
}