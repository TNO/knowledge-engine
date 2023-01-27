package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

/**
 * This fundamental error occurs when the matches do not contain all possible
 * matches, but for example only the biggest once.
 * 
 * @author nouwtb
 *
 */
public class FundamentalErrorTest {

	private static RuleStore store;

	@BeforeAll
	public static void setup() {
		store = new RuleStore();
		store.addRule(new Rule(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <isIn> ?z"), new TriplePattern("?s <hasV> ?v"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"s", "z", "v"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,<douche>,\"21.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						"<sensor2>,<glasbak>,\"15.0\"^^<http://www.w3.org/2001/XMLSchema#float>",
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s <isIn> ?z"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"s", "z"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,<badkamer>"
						//@formatter:on
				}))));
	}

	@Test
	public void testFundamentalError() throws InterruptedException, ExecutionException {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?s <isIn> ?z"));
		objective.add(new TriplePattern("?s <hasV> ?v"));

		ProactiveRule r = new ProactiveRule(objective, new HashSet<TriplePattern>());
		store.addRule(r);

		ReasonerPlan plan = new ReasonerPlan(this.store, r);

		// Start reasoning

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		binding2.put("z", "<badkamer>");
		bs.add(binding2);

		TaskBoard tb;
		while ((tb = plan.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}

		BindingSet bind = plan.getResults();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

}
