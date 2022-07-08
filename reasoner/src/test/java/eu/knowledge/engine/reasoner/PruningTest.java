package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

@TestInstance(Lifecycle.PER_CLASS)
public class PruningTest {

	private static class MyBindingSetHandler implements TransformBindingSetHandler {

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
	private Rule isInRoomRule;
	private Rule grandParentRule;
	private Rule obsoleteRule;

	@BeforeAll
	public void init() {
		reasoner = new KeReasoner();

		Rule rule = new Rule(new HashSet<>(),
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
				})));
		reasoner.addRule(rule);

		isInRoomRule = new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?x <type> <Sensor>"), new TriplePattern("?x <isInRoom> ?y"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"x", "y"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,<room1>",
						"<sensor2>,<room2>",
						//@formatter:on
				})));

		// grandparent rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isParentOf> ?y"));
		antecedent.add(new TriplePattern("?y <isParentOf> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isGrandParentOf> ?z"));
		grandParentRule = new Rule(antecedent, consequent);

		Set<TriplePattern> obsoleteAntecedent = new HashSet<>(Arrays
				.asList(new TriplePattern("?d <hasGPSCoordinates> ?coords"), new TriplePattern("?d <type> <Device>")));
		Set<TriplePattern> obsoleteConsequent = new HashSet<>(Arrays.asList(new TriplePattern("?d <isInRoom> ?rm")));

		this.obsoleteRule = new Rule(obsoleteAntecedent, obsoleteConsequent);

	}

	@Test
	public void testBackwardSingleChild() {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root
				.findAntecedentCoverage(root.getAntecedentNeighbors());
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertTrue(bind.isEmpty());
	}

	@Test
	public void testBackwardMultipleChildren() {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));
		objective.add(new TriplePattern("?p <hasOwner> ?r")); // knowledge gap

		reasoner.addRule(this.isInRoomRule);

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root
				.findAntecedentCoverage(root.getAntecedentNeighbors());
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertTrue(bind.isEmpty());
	}

	@Test
	public void test() {

		TriplePattern tp1 = new TriplePattern("?x <isGrandParentOf> ?z");
		TriplePattern tp2 = new TriplePattern("?x <hasName> ?n");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(tp1, tp2)), new HashSet<>(), aBindingSetHandler));
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
		assertNull(aBindingSetHandler.getBindingSet());
	}

	@Test
	public void testBackwardPrune() {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));
//		objective.add(new TriplePattern("?p <hasOwner> ?o")); //knowledge gap

		reasoner.addRule(this.obsoleteRule); // should be removed
		reasoner.addRule(this.isInRoomRule);

		String gp = "?d <hasGPSCoordinates> ?coords";
		Rule r = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern(gp))));
		reasoner.addRule(r);

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println("Before prune");
		System.out.println(root);

//		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root
//				.findAntecedentCoverage(root.getAntecedentNeighbors());

		root.prune();

		System.out.println("After prune");
		System.out.println(root);

		assertTrue(root.isAntecedentFullyCovered());

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());
	}

}
