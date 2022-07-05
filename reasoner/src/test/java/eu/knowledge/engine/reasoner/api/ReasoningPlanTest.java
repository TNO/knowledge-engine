package eu.knowledge.engine.reasoner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.ReactiveRule;
import eu.knowledge.engine.reasoner.ReasonerPlan;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

public class ReasoningPlanTest {

	private static final String TEST_RULES = "/reasoningplantest.rls";
	private static final String TEST_RULES_2 = "/reasoningplantest2.rls";

	@Test
	public void test() throws IOException, InterruptedException, ExecutionException {

		// load the rules
		RuleStore store = new RuleStore();

		List<ReactiveRule> someRules = Rule.read(TEST_RULES);

		ReactiveRule first = someRules.get(0);

		first.backwardBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
				"a", "b", "c"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<a1>,<b1>,<c1>",
				"<a2>,<b2>,<c2>",
				//@formatter:on
		}));

		ReactiveRule second = someRules.get(1);
		second.backwardBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
				"x", "y", "z"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<x1>,<y1>,<z1>",
				"<x2>,<y2>,<z2>",
				"<x3>,<y3>,<z3>",
				//@formatter:on
		}));

		store.addRules(new HashSet<>(someRules));

		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <pred1> ?y"));
		antecedent.add(new TriplePattern("?y <pred2> ?z"));
		Set<TriplePattern> consequent = new HashSet<>();
		ProactiveRule rule = new ProactiveRule(antecedent, consequent);
		store.addRule(rule);

		assertEquals(5, store.getRules().size());

		for (Rule r : store.getRules()) {
			store.getAntecedentNeighbors(r);
			store.getConsequentNeighbors(r);
		}

		store.printGraphVizCode();

		ReasonerPlan plan = new ReasonerPlan(store, rule);

		plan.optimize();

		plan.execute(new BindingSet());

		BindingSet bs = plan.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		assertEquals(5, bs.size());

		System.out.println(bs);
	}

}
