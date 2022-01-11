package eu.knowledge.engine.reasoner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class ForwardTest {

	public static class MyBindingSetHandler implements BindingSetHandler {

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
	
	@Before
	public void init() {
		reasoner = new KeReasoner();

		// grandparent rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isParentOf> ?y"));
		antecedent.add(new TriplePattern("?y <isParentOf> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isGrandParentOf> ?z"));
		Rule grandParent = new Rule(antecedent, consequent);
		reasoner.addRule(grandParent);

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
		

	}

	@Test
	public void test() {

		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler));
		reasoner.addRule(this.optionalRule);
		
		
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

		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), new BindingSetHandler() {

			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {
				fail("An empty bindingset should not be handled.");

				CompletableFuture<BindingSet> future = new CompletableFuture<>();
				future.complete(new BindingSet());
				return future;
			}

		}));

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

		System.out.println("Result: none (expected)");
	}

//	@Ignore
	@Test
	public void testMultipleLeafs() {

		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");
		MyBindingSetHandler aBindingSetHandler1 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler1));

		TriplePattern tp2 = new TriplePattern("?a <isGrandParentOf> ?b");
		MyBindingSetHandler aBindingSetHandler2 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp2)), new HashSet<>(), aBindingSetHandler2));
		reasoner.addRule(this.optionalRule);
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
		TriplePattern tp = new TriplePattern("?x <isGrandParentOf> ?z");
		TriplePattern tp2 = new TriplePattern("?x <hasName> ?n");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp, tp2)), new HashSet<>(), aBindingSetHandler));
		reasoner.addRule(this.optionalRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		TaskBoard taskboard = new TaskBoard();

		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard /*null*/);

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
}
