package eu.knowledge.engine.reasoner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.graph.PrefixMappingZero;
import org.apache.jena.sparql.util.FmtUtils;
import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;

public class ForwardTest {

	private static class MyBindingSetHandler implements BindingSetHandler {

		private BindingSet bs;

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			this.bs = bs;

			CompletableFuture<BindingSet> future = new CompletableFuture<>();
			future.complete(bs);
			return future;
		}

		public BindingSet getBindingSet() {
			return bs;
		}

	}

	private KeReasoner reasoner;

	private Rule optionalRule;

	private Rule grandParentRule;

	private Rule converterRule;

	@Before
	public void init() {
		reasoner = new KeReasoner();

		// grandparent rule
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
				// @formatter:offt
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
				new HashSet<>(Arrays.asList(new TriplePattern("?x <hasValInC> ?z"))), new BindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Float celcius = (Float) ((Node_Literal) b.get("y")).getLiteralValue();
							bindings.append("z:" + convert(celcius) + ",x:"
									+ FmtUtils.stringForNode(b.get("x"), new PrefixMappingZero()) + "|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();
						future.complete(bindingSet);
						return future;
					}

					public float convert(float fahrenheit) {
						return ((fahrenheit - 32) * 5) / 9;
					}

				});

	}

	@Test
	public void test() {

		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler));
		reasoner.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		TaskBoard taskboard = new TaskBoard();

		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);

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

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			taskboard.executeScheduledTasks();
		}

		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
		assertNotNull(aBindingSetHandler.getBindingSet());
		assertTrue(!aBindingSetHandler.getBindingSet().isEmpty());
	}

	@Test
	public void testDoNotHandleEmptyBindingSets() {

		TriplePattern tp = new TriplePattern("<barry> <isGrandParentOf> ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler() {

			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {
				fail("An empty bindingset should not be handled.");

				CompletableFuture<BindingSet> future = new CompletableFuture<>();
				future.complete(new BindingSet());
				return future;
			}

		};
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler));
		reasoner.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));
		TaskBoard taskboard = new TaskBoard();
		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);

		System.out.println(rn);

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

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			taskboard.executeScheduledTasks();
		}

		System.out.println("Result: " + aBindingSetHandler.getBindingSet() + " (expected null)");
		assertEquals(aBindingSetHandler.getBindingSet(), null);
	}

	@Test
	public void testMultipleLeafs() {

		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler1));

		TriplePattern tp2 = new TriplePattern("?a <isGrandParentOf> ?b");
		MyBindingSetHandler aBindingSetHandler2 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp2)), new HashSet<>(), aBindingSetHandler2));
		reasoner.addRule(this.optionalRule);
		reasoner.addRule(grandParentRule);
		Set<TriplePattern> aPremise = new HashSet<>();
		aPremise.add(new TriplePattern("?x <isParentOf> ?y"));

		TaskBoard taskboard = new TaskBoard();

		ReasoningNode rn = reasoner.forwardPlan(aPremise, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);

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

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			System.out.println(taskboard);
			System.out.println();
			taskboard.executeScheduledTasks();
		}

		assertTrue(aBindingSetHandler2.getBindingSet() != null);
		assertTrue(aBindingSetHandler1.getBindingSet() != null);

		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());
		assertFalse(aBindingSetHandler2.getBindingSet().isEmpty());

		System.out.println("Result1: " + aBindingSetHandler1.getBindingSet());
		System.out.println("Result2: " + aBindingSetHandler2.getBindingSet());
	}

	@Test
	public void testBackwardChainingDuringForwardChaining() {
		TriplePattern tp = new TriplePattern("?x <isParentOf> ?z");
		TriplePattern tp2 = new TriplePattern("?x <hasName> ?n");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp, tp2)), new HashSet<>(), aBindingSetHandler));
		reasoner.addRule(this.optionalRule);
		reasoner.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		TaskBoard taskboard = new TaskBoard();

		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard /* null */);

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

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			System.out.println(taskboard);
			System.out.println();
			taskboard.executeScheduledTasks();
		}

		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
		assertNotNull(aBindingSetHandler.getBindingSet());
		assertTrue(!aBindingSetHandler.getBindingSet().isEmpty());
	}

	@Test
	public void testNoBackwardChainingDuringForwardChainingIfFullMatch() {

		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12)), new HashSet<>(), aBindingSetHandler1));
		reasoner.addRule(grandParentRule);
		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);

		TriplePattern tp31 = new TriplePattern("?s <hasMeasuredValue> ?v");
		TriplePattern tp32 = new TriplePattern("?s <type> <Sensor>");
		reasoner.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31, tp32)),
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

		TaskBoard taskboard = new TaskBoard();
		ReasoningNode rn = reasoner.forwardPlan(premise, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
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

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			System.out.println(taskboard);
			System.out.println();
			taskboard.executeScheduledTasks();
		}

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertTrue(!aBindingSetHandler1.getBindingSet().isEmpty());

		assertEquals(1, aBindingSetHandler1.getBindingSet().size());
	}

	@Test
	public void testBackwardChainingDuringForwardChainingIfPartialMatch() {

		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasMeasuredValue> ?value");
		TriplePattern tp13 = new TriplePattern("?sens <isInRoom> ?room");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		reasoner.addRule(
				new Rule(new HashSet<>(Arrays.asList(tp11, tp12, tp13)), new HashSet<>(), aBindingSetHandler1));
		reasoner.addRule(grandParentRule);
		TriplePattern tp21 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp22 = new TriplePattern("?sensor <hasMeasuredValue> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp21);
		premise.add(tp22);

		TriplePattern tp31 = new TriplePattern("?s <isInRoom> ?r");
		reasoner.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31)),
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

		TaskBoard taskboard = new TaskBoard();
		ReasoningNode rn = reasoner.forwardPlan(premise, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
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

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			System.out.println(taskboard);
			System.out.println();
			taskboard.executeScheduledTasks();
		}

		System.out.println(aBindingSetHandler1.getBindingSet());
		assertTrue(!aBindingSetHandler1.getBindingSet().isEmpty());
		assertEquals(aBindingSetHandler1.getBindingSet().size(), 1);
	}

	@Test
	public void testPublishedValuesShouldRemainAccessible() {

		TriplePattern tp11 = new TriplePattern("?sens <type> <Sensor>");
		TriplePattern tp12 = new TriplePattern("?sens <hasValInC> ?value");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp11, tp12)), new HashSet<>(), aBindingSetHandler1));

		reasoner.addRule(this.converterRule);

		TriplePattern tp31 = new TriplePattern("?sensor <type> <Sensor>");
		TriplePattern tp32 = new TriplePattern("?sensor <hasValInF> ?value");
		Set<TriplePattern> premise = new HashSet<>();
		premise.add(tp31);
		premise.add(tp32);

		class StoreBindingSetHandler implements BindingSetHandler {

			private BindingSet b = null;

			public StoreBindingSetHandler(BindingSet aB) {
				this.b = aB;
			}

			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {
				CompletableFuture<BindingSet> future = new CompletableFuture<>();
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

		reasoner.addRule(
				new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(tp31, tp32)), new StoreBindingSetHandler(bs)));

		TaskBoard aTaskboard = new TaskBoard();
		ReasoningNode rn = reasoner.forwardPlan(premise, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, aTaskboard);

		System.out.println(rn);
		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			System.out.println(aTaskboard);
			System.out.println();
			aTaskboard.executeScheduledTasks();
		}

		System.out.println(aBindingSetHandler1.getBindingSet());

		assertFalse(aBindingSetHandler1.getBindingSet().isEmpty());
	}

}
