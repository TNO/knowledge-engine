package eu.knowledge.engine.reasoner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.RuleStore;

public class ReasoningPlanTest {

	private static final String TEST_RULES = "/reasoningplantest.rls";

	@Test
	public void test() throws IOException, InterruptedException, ExecutionException {

		// load the rules
		RuleStore store = new RuleStore();
		store.read(TEST_RULES);

		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <pred1> ?y"));
		antecedent.add(new TriplePattern("?y <pred2> ?z"));
		Set<TriplePattern> consequent = new HashSet<>();
		ProactiveRule rule = new ProactiveRule(antecedent, consequent);
		store.addRule(rule);

		antecedent = new HashSet<>();
		consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <pred1> ?y"));
		consequent.add(new TriplePattern("?y <pred2> ?z"));

		ReactiveRule reactiveRule = new ReactiveRule(antecedent, consequent);
		store.addRule(reactiveRule);

		assertEquals(4, store.getRules().size());

		for (Rule r : store.getRules()) {
			store.getAntecedentNeighbors(r);
			store.getConsequentNeighbors(r);
		}

		store.printGraphVizCode();

		ReasonerPlan plan = new ReasonerPlan(store, rule);

		plan.optimize();

		plan.execute(new BindingSet());
	}

}
