package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

@TestInstance(Lifecycle.PER_CLASS)
public class MinimalTest {

	private KeReasoner reasoner;

	@BeforeAll
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasoner();
		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?a <type> <Sensor>"), new TriplePattern("?a <hasValInC> ?b"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"a", "b"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,22",
						"<sensor2>,21",
						//@formatter:on
				}))));
	}

	@Test
	public void testConverter() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testConverterVariableType() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> ?t"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("p", "<sensor1>");
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
