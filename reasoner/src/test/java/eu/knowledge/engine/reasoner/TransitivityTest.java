package eu.knowledge.engine.reasoner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

@TestInstance(Lifecycle.PER_CLASS)
public class TransitivityTest {

	private KeReasoner reasoner;

	@BeforeAll
	public void init() {
		reasoner = new KeReasoner();

		// transitivity rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x <isVoorouderVan> ?y"));
		antecedent.add(new TriplePattern("?y <isVoorouderVan> ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x <isVoorouderVan> ?z"));
		ReactiveRule transitivity = new ReactiveRule(antecedent, consequent);
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

		ReactiveRule rule = new ReactiveRule(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?a <isVoorouderVan> ?b"))), aBindingSetHandler);

		reasoner.addRule(rule);
	}

	@Test
	public void test() {
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x <isVoorouderVan> ?y"));
		TaskBoard taskboard = new TaskBoard();
		ReasoningNode rn = reasoner.backwardPlan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		BindingSet result = null;

		int nrOfExpectedBindings = 15;

		System.out.println(rn);
		while ((result = rn.continueBackward(new BindingSet())) == null) {
			System.out.println(rn);
			taskboard.executeScheduledTasks();
		}

		System.out.println("Size (should be " + nrOfExpectedBindings + "): " + result.size());
		System.out.println("Result: " + result);
		assertEquals(nrOfExpectedBindings, result.size());

	}

}
