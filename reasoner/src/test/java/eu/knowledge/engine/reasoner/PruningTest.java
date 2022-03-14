package eu.knowledge.engine.reasoner;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class PruningTest {

	private KeReasoner reasoner;
	private Rule isInRoomRule;

	@Before
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

	}

	@Test
	public void testSingleChild() {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root.findAntecedentCoverage();
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
	public void testMultipleChildren() {
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));
		objective.add(new TriplePattern("?p <isInRoom> ?r"));
		objective.add(new TriplePattern("?p <hasOwner> ?r"));	//knowledge gap

		reasoner.addRule(this.isInRoomRule);
		
		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		Map<TriplePattern, Set<ReasoningNode>> findAntecedentCoverage = root.findAntecedentCoverage();
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
	
}
