package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;

@TestInstance(Lifecycle.PER_CLASS)
public class TransitivityTest {

	private RuleStore store;

	@BeforeAll
	public void init() {
		store = new RuleStore();

		// transitivity rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isVoorouderVan> ?y"));
		antecedent.add(new TriplePattern("?y <isVoorouderVan> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isVoorouderVan> ?z"));
		Rule transitivity = new Rule(antecedent, consequent);
		store.addRule(transitivity);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
				"a", "b"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<barry>,<fenna>",
				"<janny>,<barry>",
				"<fenna>,<benno>",
				"<benno>,<loes>",
				"<loes>,<hendrik>",
				//@formatter:on
		}));

		Rule rule = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?a <isVoorouderVan> ?b"))),
				aBindingSetHandler);

		store.addRule(rule);
	}

	@Disabled // TODO not yet implemented
	@Test
	public void test() throws InterruptedException, ExecutionException {
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isVoorouderVan> ?y"));

		ProactiveRule startRule = new ProactiveRule(aGoal, new HashSet<>());

		store.addRule(startRule);

		TaskBoard taskboard = new TaskBoard();
		ReasonerPlan rn = new ReasonerPlan(store, startRule);

		BindingSet result = null;

		int nrOfExpectedBindings = 15;

		System.out.println(rn);
		rn.execute(new BindingSet());

		result = rn.getStartNode().getIncomingAntecedentBindingSet().toBindingSet();

		System.out.println("Size (should be " + nrOfExpectedBindings + "): " + result.size());
		System.out.println("Result: " + result);
		assertEquals(nrOfExpectedBindings, result.size());
	}

}
