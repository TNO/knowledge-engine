package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.Table;

@TestInstance(Lifecycle.PER_CLASS)
public class MinimalTest {

	private RuleStore store;

	@BeforeAll
	public void init() throws URISyntaxException {
		// Initialize
		store = new RuleStore();
		store.addRule(new Rule(new HashSet<>(),
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
	public void testConverter() throws InterruptedException, ExecutionException {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> <Sensor>"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());
		this.store.addRule(startRule);

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(this.store, startRule);
		this.store.printGraphVizCode(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet bind = root.getResults();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testConverterVariableType() throws InterruptedException, ExecutionException {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p <type> ?t"));
		objective.add(new TriplePattern("?p <hasValInC> ?q"));

		ProactiveRule startRule = new ProactiveRule(objective, new HashSet<>());
		this.store.addRule(startRule);

		// Start reasoning
		ReasonerPlan root = new ReasonerPlan(store, startRule);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("p", "<sensor1>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = root.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet bind = root.getResults();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());
	}
}
