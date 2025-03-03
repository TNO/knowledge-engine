package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.SinkBindingSetHandler;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.Table;

@Disabled("Until the optimize() method is available.")
@TestInstance(Lifecycle.PER_CLASS)
public class PruningTest {

	private static final Logger LOG = LoggerFactory.getLogger(PruningTest.class);

	private static class MyBindingSetHandler implements SinkBindingSetHandler {
		protected static final Logger LOG = LoggerFactory.getLogger(PruningTest.class);

		private BindingSet bs;

		@Override
		public CompletableFuture<Void> handle(BindingSet bs) {

			this.bs = bs;

			CompletableFuture<Void> future = new CompletableFuture<>();

			future.handle((r, e) -> {

				if (r == null) {
					LOG.error("An exception has occured while handling binding set", e);
					return null;
				} else {
					return r;
				}
			});
			future.complete((Void) null);
			return future;
		}

		public BindingSet getBindingSet() {
			return bs;
		}

	}

	private RuleStore store;
	private Rule isInRoomRule;
	private Rule grandParentRule;
	private Rule obsoleteRule;

	@BeforeAll
	public void init() {
		store = new RuleStore();

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
		store.addRule(rule);

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

	@Disabled("Until optimize() method is available.")
	@Test
	public void testBackwardSingleChild() throws InterruptedException, ExecutionException {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		TriplePattern t1 = new TriplePattern("?p <type> <Sensor>");
		objective.add(t1);
		TriplePattern t2 = new TriplePattern("?p <hasValInC> ?q");
		objective.add(t2);
		TriplePattern t3 = new TriplePattern("?p <isInRoom> ?r");
		objective.add(t3);

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(startRule);
		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(store, startRule);

		/*
		 * var coverage =
		 * root.getStartNode().findAntecedentCoverage(root.getStartNode().
		 * getAntecedentNeighbors());
		 * 
		 * assertFalse(coverage.get(t1).isEmpty());
		 * assertFalse(coverage.get(t2).isEmpty());
		 * assertTrue(coverage.get(t3).isEmpty());
		 * 
		 * 
		 * BindingSet bs = new BindingSet(); Binding binding2 = new Binding();
		 * bs.add(binding2);
		 * 
		 * while (!root.execute(bs)) { taskboard.executeScheduledTasks().get(); }
		 * 
		 * BindingSet bind =
		 * root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();
		 * System.out.println("bindings: " + bind); assertTrue(bind.isEmpty());
		 */
	}

	@Test
	public void testBackwardMultipleChildren() throws InterruptedException, ExecutionException {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));
		TriplePattern t4 = new TriplePattern("?p <hasOwner> ?r");
		objective.add(t4); // knowledge gap

		store.addRule(this.isInRoomRule);

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(startRule);
		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(store, startRule);
		/*
		 * var coverage =
		 * root.getStartNode().findAntecedentCoverage(root.getStartNode().
		 * getAntecedentNeighbors());
		 * 
		 * assertTrue(coverage.get(t4).isEmpty());
		 * 
		 * BindingSet bs = new BindingSet(); Binding binding2 = new Binding();
		 * bs.add(binding2);
		 * 
		 * root.optimize();
		 * 
		 * while (!root.execute(bs)) { taskboard.executeScheduledTasks().get(); }
		 * 
		 * BindingSet bind =
		 * root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();
		 * 
		 * System.out.println("bindings: " + bind); assertTrue(bind.isEmpty());
		 */
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {

		TriplePattern tp1 = new TriplePattern("?x <isGrandParentOf> ?z");
		TriplePattern tp2 = new TriplePattern("?x <hasName> ?n");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp1, tp2)), aBindingSetHandler));
		store.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		ProactiveRule startRule = new ProactiveRule(new HashSet<>(), aGoal);
		store.addRule(startRule);
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan rn = new ReasonerPlan(store, startRule);

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
		/*
		 * while (!rn.execute(bs)) { taskboard.executeScheduledTasks().get(); }
		 */
		System.out.println("Result: " + aBindingSetHandler.getBindingSet());
		assertNull(aBindingSetHandler.getBindingSet());
	}

	@Test
	public void testBackwardPrune() throws InterruptedException, ExecutionException {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));
//		objective.add(new TriplePattern("?p <hasOwner> ?o")); //knowledge gap

		store.addRule(this.obsoleteRule); // should be removed
		store.addRule(this.isInRoomRule);

		String gp = "?d <hasGPSCoordinates> ?coords";
		Rule r = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern(gp))));
		store.addRule(r);

		TaskBoard taskboard = new TaskBoard();

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(startRule);

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(store, startRule);
		LOG.info("Before prune");
		/*
		 * root.optimize();
		 * 
		 * LOG.info("After prune");
		 * 
		 * assertTrue(root.getStartNode().isAntecedentFullyCovered());
		 * 
		 * BindingSet bs = new BindingSet(); Binding binding2 = new Binding();
		 * bs.add(binding2);
		 * 
		 * while (!root.execute(bs)) { taskboard.executeScheduledTasks().get(); }
		 * 
		 * BindingSet bind =
		 * root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();
		 * 
		 * LOG.info("bindings: " + bind); assertFalse(bind.isEmpty());
		 */
	}

}
