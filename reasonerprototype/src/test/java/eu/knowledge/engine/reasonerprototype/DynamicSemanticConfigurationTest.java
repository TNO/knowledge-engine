package eu.knowledge.engine.reasonerprototype;

import static org.junit.Assert.assertFalse;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasonerprototype.Rule.MatchStrategy;
import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Literal;

public class DynamicSemanticConfigurationTest {

	private KeReasoner reasoner;

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasoner();
		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(
						Arrays.asList(new TriplePattern("?id type Target"), new TriplePattern("?id hasName ?name"))),
				new BindingSetHandler() {

					private Table data = new Table(new String[] {
				//@formatter:off
							"?id", "?name"
							//@formatter:on
					}, new String[] {
				//@formatter:off
							"<https://www.tno.nl/target0>,Eek",
							"<https://www.tno.nl/target1>,Bla",
							//@formatter:on
					});

					@Override
					public BindingSet handle(BindingSet bs) {

						BindingSet newBS = new BindingSet();
						if (!bs.isEmpty()) {

							for (Binding b : bs) {

								if (!b.isEmpty()) {
									Set<Map<String, String>> map = data.query(b.toMap());
									if (!map.isEmpty())
										newBS.addAll(map);
								} else {
									newBS.addAll(this.data.getData());
								}
							}
						} else {
							newBS.addAll(this.data.getData());
						}
						return newBS;
					}

				}));

		reasoner.addRule(new Rule(
				new HashSet<>(Arrays.asList(new TriplePattern("?id type Target"),
						new TriplePattern("?id hasCountry Russia"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?id type HighValueTarget")))));

		reasoner.addRule(new Rule(
				new HashSet<>(
						Arrays.asList(new TriplePattern("?id type Target"), new TriplePattern("?id hasName ?name"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?id hasCountry ?c"))), new BindingSetHandler() {

					@Override
					public BindingSet handle(BindingSet bs) {
						assert bs.iterator().hasNext();
						BindingSet newBS = new BindingSet();
						for (Binding incomingB : bs) {
							Binding resultBinding = new Binding();

							Literal id;
							if (incomingB.containsKey("?id")
									&& incomingB.get("?id").equals(new Literal("<https://www.tno.nl/target1>"))) {

								id = incomingB.get("?id");
								resultBinding.put("?id", id.getValue());
								resultBinding.put("?c", "Russia");
							} else if (incomingB.containsKey("?id")
									&& incomingB.get("?id").equals(new Literal("<https://www.tno.nl/target0>"))) {
								id = incomingB.get("?id");
								resultBinding.put("?id", id.getValue());
								resultBinding.put("?c", "Holland");
							} else {
								id = incomingB.get("?id");
								resultBinding.put("id", id.getValue());
								resultBinding.put("c", "Belgium");
							}
							newBS.add(resultBinding);
						}
						return newBS;
					}
				}));
	}

	@Test
	public void test() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?id type HighValueTarget"));
		objective.add(new TriplePattern("?id hasName ?name"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES);
		System.out.println(root);

		BindingSet bs = new BindingSet();

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}
}
