package eu.knowledge.engine.reasoner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class TransitivityTest {

	private KeReasoner reasoner;

	@Before
	public void init() {
		reasoner = new KeReasoner();

		// transitivity rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isVoorouderVan> ?y"));
		antecedent.add(new TriplePattern("?y <isVoorouderVan> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isVoorouderVan> ?z"));
		Rule transitivity = new Rule(antecedent, consequent);
		reasoner.addRule(transitivity);

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

		reasoner.addRule(rule);
	}

	@Ignore
	@Test
	public void test() {
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isVoorouderVan> ?y"));
		TaskBoard taskboard = new TaskBoard();
		ReasoningNode rn = reasoner.backwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		BindingSet result = null;

		System.out.println(rn);
		while ((result = rn.continueBackward(new BindingSet())) == null) {
			System.out.println(rn);
			taskboard.executeScheduledTasks();
		}

		System.out.println("Result: " + result);
	}

}
