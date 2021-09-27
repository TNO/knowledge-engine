package eu.knowledge.engine.reasonerprototype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasonerprototype.BindingSetHandler;
import eu.knowledge.engine.reasonerprototype.KeReasonerAlt;
import eu.knowledge.engine.reasonerprototype.NodeAlt;
import eu.knowledge.engine.reasonerprototype.RuleAlt;
import eu.knowledge.engine.reasonerprototype.TaskBoard;
import eu.knowledge.engine.reasonerprototype.api.Binding;
import eu.knowledge.engine.reasonerprototype.api.BindingSet;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern;
import eu.knowledge.engine.reasonerprototype.api.Util;
import eu.knowledge.engine.reasonerprototype.api.TriplePattern.Variable;

public class KeReasonerTest2 {

	private KeReasonerAlt reasoner;

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasonerAlt();
		reasoner.addRule(new RuleAlt(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?a type Sensor"), new TriplePattern("?a hasValInC ?b"))),
				new BindingSetHandler() {

					private Table data = new Table(new String[] {
				//@formatter:off
							"?a", "?b"
							//@formatter:on
					}, new String[] {
				//@formatter:off
							"<sensor1>,22",
							"<sensor2>,21",
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

		reasoner.addRule(new RuleAlt(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?e type Sensor"), new TriplePattern("?e hasValInF ?f"))),
				new BindingSetHandler() {

					private Table data = new Table(new String[] {
				//@formatter:off
					"?e", "?f"
					//@formatter:on
					}, new String[] {
				//@formatter:off
					"<sensor3>,69",
					"<sensor4>,71",
					//@formatter:on
					});

					@Override
					public BindingSet handle(BindingSet bs) {

						BindingSet newBS = new BindingSet();

						if (!bs.isEmpty()) {

							for (Binding b : bs) {

								if (!b.isEmpty()) {
									Set<Map<String, String>> map = data.query(b.toMap());
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

		reasoner.addRule(new RuleAlt(new HashSet<>(Arrays.asList(new TriplePattern("?s type Sensor"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s type Device")))));

		reasoner.addRule(new RuleAlt(new HashSet<>(Arrays.asList(new TriplePattern("?x hasValInF ?y"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?x hasValInC ?z"))), new BindingSetHandler() {

					@Override
					public BindingSet handle(BindingSet bs) {
						String bindings = "";
						for (Binding b : bs) {
							Float celcius = Float.valueOf(b.get(new Variable("?y")).getValue());
							bindings += "?z:" + convert(celcius) + ",?x:" + b.get(new Variable("?x")).getValue() + "|";
						}
						BindingSet bindingSet = Util.toBindingSet(bindings);
						return bindingSet;
					}

					public float convert(float fahrenheit) {
						return ((fahrenheit - 32) * 5) / 9;
					}

				}));
	}

	@Test
	public void testRequestNonExistingData() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Device"));
		objective.add(new TriplePattern("?p hasValInC ?q"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		binding2.put("?q", "21");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertTrue(bind.isEmpty());

	}

	@Test
	public void testConverter() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Sensor"));
		objective.add(new TriplePattern("?p hasValInC ?q"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("?p", "<sensor1>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testMoreThanOneInputBinding() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Device"));
		objective.add(new TriplePattern("?p hasValInC ?q"));
		// objective.add(new TriplePattern("?p hasValInT ?q")); //TODO this still does
		// not work

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("?p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testTwoPropsToAndFromTheSameVars() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Device"));
		objective.add(new TriplePattern("?p hasValInC ?q"));
		objective.add(new TriplePattern("?p nonExistentProp ?q"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		BindingSet bs = new BindingSet();
//		Binding binding = new Binding();
//		binding.put("?q", "22");
//		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		binding2.put("?q", "22");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertTrue(bind.isEmpty());
	}

	@Test
	public void testVariableMatchesLiteralInGraphPattern() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type ?t"));
		objective.add(new TriplePattern("?p hasValInC ?q"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		binding2.put("?q", "22");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}

	@Test
	public void testVariableAsPredicate() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Sensor"));
		objective.add(new TriplePattern("?p ?pred 21.666666"));
//		objective.add(new TriplePattern("?p ?pred 22"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}
	
	@Test
	public void testVariableAsPredicate2() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Sensor"));
//		objective.add(new TriplePattern("?p ?pred 21.666666"));
		objective.add(new TriplePattern("?p ?pred 22"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);
		System.out.println(root);

		//empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueReasoning(bs)) == null) {
			System.out.println(root);
			TaskBoard.instance().executeScheduledTasks();
		}
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty()); // TODO THIS ONE SHOULD CONTAIN ONLY sensor1
	}

}
