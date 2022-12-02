package eu.knowledge.engine.reasoner2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.Table;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class LoopTest {

	private RuleStore store;

	@BeforeAll
	public void init() {
		store = new RuleStore();

		// transitivity rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isAncestorOf> ?y"));
		antecedent.add(new TriplePattern("?y <isAncestorOf> ?z"));
		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isSmurfOf> ?z"));
		Rule transitivityRule = new Rule(antecedent, consequent);
		store.addRule(transitivityRule);

		// smurf rule
		antecedent = new HashSet<TriplePattern>();
		antecedent.add(new TriplePattern("?x <isSmurfOf> ?y"));
		consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isAncestorOf> ?y"));
		Rule smurfRule = new Rule(antecedent, consequent);
		store.addRule(smurfRule);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(
				new Table(new String[] { "a", "b" }, new String[] {
				//@formatter:off
				"<barry>,<fenna>",
				"<janny>,<barry>",
				"<fenna>,<benno>",
				"<benno>,<loes>",
				"<loes>,<hendrik>",
				//@formatter:on
				}));

		Rule rule = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?a <isSmurfOf> ?b"))),
				aBindingSetHandler);

		store.addRule(rule);
	}

	@Test
	public void test() {
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isAncestorOf> ?y"));
		ProactiveRule startRule = new ProactiveRule(aGoal, new HashSet<>());
		store.addRule(startRule);

		ReasonerPlan plan = new ReasonerPlan(store, startRule);

		store.printGraphVizCode(plan);

		plan.execute(new BindingSet());

		BindingSet result = plan.getResults();

		assertEquals(15, result.size());
	}

}
