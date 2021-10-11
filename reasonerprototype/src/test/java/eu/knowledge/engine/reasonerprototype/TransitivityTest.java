package eu.knowledge.engine.reasonerprototype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasonerprototype.Rule.MatchStrategy;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;

public class TransitivityTest {

	private KeReasoner reasoner;

	@Before
	public void init() {
		reasoner = new KeReasoner();

		// transitivity rule
		Set<TriplePattern> antecedent = new HashSet<>();
		antecedent.add(new TriplePattern("?x isVoorouderVan ?y"));
		antecedent.add(new TriplePattern("?y isVoorouderVan ?z"));

		Set<TriplePattern> consequent = new HashSet<>();
		consequent.add(new TriplePattern("?x isVoorouderVan ?z"));
		Rule transitivity = new Rule(antecedent, consequent);
		reasoner.addRule(transitivity);

		// data rule
		DataBindingSetHandler aBindingSetHandler = new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
				"?a", "?b"
				//@formatter:on
		}, new String[] {
				//@formatter:off
				"<barry>,<fenna>",
				"<janny>,<barry>",
				//@formatter:on
		}));

		Rule rule = new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?a isVoorouderVan ?b"))),
				aBindingSetHandler);

		reasoner.addRule(rule);
	}

	@Test
	public void test() {
		Set<TriplePattern> aGoal = new HashSet<>();
		aGoal.add(new TriplePattern("?x isVoorouderVan ?y"));

		ReasoningNode rn = reasoner.plan(aGoal, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);
		BindingSet result = null;

		do {
			System.out.println(rn);
		} while ((result = rn.executeBackward(new BindingSet())) != null);

		System.out.println("Result: " + result);
	}

}
