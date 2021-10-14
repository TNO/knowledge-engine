package eu.knowledge.engine.reasonerprototype;

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

		ReasoningNode rn = reasoner.forwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);

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
			TaskBoard.instance().executeScheduledTasks();
		}

		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
	}

}
