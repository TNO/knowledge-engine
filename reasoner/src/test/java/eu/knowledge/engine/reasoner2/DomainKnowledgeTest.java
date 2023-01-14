/**
 * 
 */
package eu.knowledge.engine.reasoner2;

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
 * 
 * Test the usage of domain knowledge with transitive relations like
 * owl:subClassOf.
 * 
 * @author nouwtb
 *
 */
public class DomainKnowledgeTest {

	private static RuleStore store;

	@BeforeAll
	public static void setup() {
		store = new RuleStore();

		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?s <type> ?t"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"s", "t"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<barry>,<human>",
						"<bobby>,<dog>",
						"<nemo>,<fish>",
						//@formatter:on
				}))));

		store.addRule(new Rule(new HashSet<>(), new HashSet<>(Arrays.asList(new TriplePattern("?t1 <subclassof> ?t2"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"t1", "t2"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<human>,<mammal>",
						"<dog>,<mammal>",
						"<mammal>,<warmblooded>",
						"<bird>,<warmblooded>",
						"<warmblooded>,<vertebrates>",
						"<vertebrates>,<animal>",
						"<invertebrates>,<animal>",
						"<fish>,<coldblooded>",
						"<reptiles>,<coldblooded>",
						"<amphibians>,<coldblooded>",
						"<coldblooded>,<vertebrates>",
						//@formatter:on
				}))));

		store.addRule(new Rule(
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> ?t"),
						new TriplePattern("?t <subclassof> <animal>"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s <type> <animal>")))));

		store.addRule(new Rule(
				new HashSet<>(Arrays.asList(new TriplePattern("?t1 <subclassof> ?t2"),
						new TriplePattern("?t2 <subclassof> ?t3"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?t1 <subclassof> ?t3")))));
	}

	@Test
	public void testSubClassOf() throws InterruptedException, ExecutionException {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?a <type> <animal>"));
		ProactiveRule r = new ProactiveRule(objective, new HashSet<TriplePattern>());

		store.addRule(r);

		ReasonerPlan plan = new ReasonerPlan(store, r);

		// Start reasoning

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);


		TaskBoard tb;
		while ((tb = plan.execute(bs)).hasTasks()) {
			tb.executeScheduledTasks().get();
		}
		store.printGraphVizCode(plan);

		BindingSet bind = plan.getResults();

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

}
