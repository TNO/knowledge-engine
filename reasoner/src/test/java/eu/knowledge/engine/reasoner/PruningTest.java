package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class PruningTest {

	private static class MyBindingSetHandler implements TransformBindingSetHandler {
		protected static final Logger LOG = LoggerFactory.getLogger(PruningTest.class);

		private BindingSet bs;

		@Override
		public CompletableFuture<BindingSet> handle(BindingSet bs) {

			this.bs = bs;

			CompletableFuture<BindingSet> future = new CompletableFuture<>();

			future.handle((r, e) -> {

				if (r == null) {
					LOG.error("An exception has occured while handling binding set", e);
					return null;
				} else {
					return r;
				}
			});
			future.complete(bs);
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

	@Test
	public void testBackwardSingleChild() throws InterruptedException, ExecutionException {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());
		store.addRule(startRule);
		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(store, startRule);
		System.out.println(root);

//		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root
//				.findAntecedentCoverage(root.getAntecedentNeighbors());
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		root.execute(bs);

		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertTrue(bind.isEmpty());
	}

	@Test
	public void testBackwardMultipleChildren() throws InterruptedException, ExecutionException {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));
		objective.add(new TriplePattern("?p <hasOwner> ?r")); // knowledge gap

		store.addRule(this.isInRoomRule);

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(store, startRule);
		System.out.println(root);

//		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root
//				.findAntecedentCoverage(root.getAntecedentNeighbors());
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		root.execute(bs);

		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertTrue(bind.isEmpty());
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {

		TriplePattern tp1 = new TriplePattern("?x <isGrandParentOf> ?z");
		TriplePattern tp2 = new TriplePattern("?x <hasName> ?n");

		MyBindingSetHandler aBindingSetHandler = new MyBindingSetHandler();
		store.addRule(new Rule(new HashSet<>(Arrays.asList(tp1, tp2)), new HashSet<>(), aBindingSetHandler));
		store.addRule(grandParentRule);
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isParentOf> ?y"));

		ProactiveRule startRule = new ProactiveRule(new HashSet<>(), aGoal);
		store.addRule(startRule);
		TaskBoard taskboard = new TaskBoard();

		ReasonerPlan rn = new ReasonerPlan(store, startRule);

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
		System.out.println("Before prune");
		System.out.println(root);

//		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root
//				.findAntecedentCoverage(root.getAntecedentNeighbors());

//		root.prune();

		System.out.println("After prune");
		System.out.println(root);

//		assertTrue(root.isAntecedentFullyCovered());

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		root.execute(bs);

		BindingSet bind = root.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());
	}

}
