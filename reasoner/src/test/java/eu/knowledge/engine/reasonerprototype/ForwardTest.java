package eu.knowledge.engine.reasonerprototype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasonerprototype.Rule.MatchStrategy;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;

public class ForwardTest {

	public static class MyBindingSetHandler implements BindingSetHandler {

		private BindingSet bs;

		@Override
		public BindingSet handle(BindingSet bs) {

			this.bs = bs;

			return new BindingSet();
		}

		public BindingSet getBindingSet() {
			return bs;
		}

	}

	private KeReasoner reasoner;

	@Before
	public void init() {
		reasoner = new KeReasoner();

		// grandparent rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x isParentOf ?y"));
		antecedent.add(new TriplePattern("?y isParentOf ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x isGrandParentOf ?z"));
		Rule grandParent = new Rule(antecedent, consequent);
		reasoner.addRule(grandParent);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
				"?a", "?b"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<barry>,<fenna>",
				"<janny>,<barry>",
				"<fenna>,<benno>",
				"<benno>,<loes>",
				//@formatter:on
		}));

		Rule rule = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?a isParentOf ?b"))),
				aBindingSetHandler);

//		reasoner.addRule(rule);
	}

	@Test
	public void test() {

		TriplePattern tp = new TriplePattern("?x isGrandParentOf ?z");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler));

		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x isParentOf ?y"));

		TaskBoard taskboard = new TaskBoard();

		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);

		System.out.println(rn);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				//@formatter:off
				"?x", "?y"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<barry>,<fenna>",
				"<janny>,<barry>",
				"<fenna>,<benno>",
				"<benno>,<loes>",
				//@formatter:on
		}).getData());

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			taskboard.executeScheduledTasks();
		}

		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
	}

	@Test
	public void testDoNotHandleEmptyBindingSets() {

		TriplePattern tp = new TriplePattern("<barry> isGrandParentOf ?z");

		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), new BindingSetHandler() {

			@Override
			public BindingSet handle(BindingSet bs) {
				assertTrue("The binding set should be empty.", bs.isEmpty());
				fail("An empty bindingset should not be handled.");
				return new BindingSet();
			}

		}));

		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x isParentOf ?y"));
		TaskBoard taskboard = new TaskBoard();
		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);

		System.out.println(rn);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				//@formatter:off
				"?x", "?y"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<fenna>,<benno>",
				"<benno>,<loes>",
				//@formatter:on
		}).getData());

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			taskboard.executeScheduledTasks();
		}

		System.out.println("Result: none (expected)");
	}

	@Test

	public void testMultipleLeafs() {

		TriplePattern tp = new TriplePattern("?x isGrandParentOf ?z");
		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp)), new HashSet<>(), aBindingSetHandler));

		TriplePattern tp2 = new TriplePattern("?a isGrandParentOf ?b");
		MyBindingSetHandler aBindingSetHandler2 = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp2)), new HashSet<>(), aBindingSetHandler2));

		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x isParentOf ?y"));

		TaskBoard taskboard = new TaskBoard();

		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);

		BindingSet bs = new BindingSet();

		bs.addAll(new Table(new String[] {
				//@formatter:off
				"?x", "?y"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<barry>,<fenna>",
				"<janny>,<barry>",
				"<fenna>,<benno>",
				"<benno>,<loes>",
				//@formatter:on
		}).getData());

		while (!rn.continueForward(bs)) {
			System.out.println(rn);
			System.out.println(taskboard);
			System.out.println();
			taskboard.executeScheduledTasks();
		}

		assertTrue(aBindingSetHandler2.getBindingSet() != null);
		assertTrue(aBindingSetHandler.getBindingSet() != null);

		assertFalse(aBindingSetHandler.getBindingSet().isEmpty());
		assertFalse(aBindingSetHandler2.getBindingSet().isEmpty());

		System.out.println("Result1: " + aBindingSetHandler.getBindingSet());
		System.out.println("Result2: " + aBindingSetHandler2.getBindingSet());
	}
}
