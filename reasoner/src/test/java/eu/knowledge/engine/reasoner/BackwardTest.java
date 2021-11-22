package eu.knowledge.engine.reasoner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.KeReasoner;
import eu.knowledge.engine.reasoner.ReasoningNode;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.TaskBoard;
import eu.knowledge.engine.reasoner.Rule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;
import eu.knowledge.engine.reasoner.api.Util;
import eu.knowledge.engine.reasoner.api.TriplePattern.Variable;

public class BackwardTest {

	private KeReasoner reasoner;

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasoner();
		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?a type Sensor"), new TriplePattern("?a hasValInC ?b"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"?a", "?b"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor1>,22",
						"<sensor2>,21",
						//@formatter:on
				}))));

		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?e type Sensor"), new TriplePattern("?e hasValInF ?f"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"?e", "?f"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor3>,69",
						"<sensor4>,71",
						//@formatter:on
				}))));

		 
		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?k type Sensor"), new TriplePattern("?k hasValInK ?w"))),
				new DataBindingSetHandler(new Table(new String[]{
					"?k", "?y"
				}, new String[] {
						"<sensor5>,295",
						"<sensor6>,294"
				}))
		));
		

		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?s type Sensor"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?s type Device")))));

		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?x hasValInF ?y"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?x hasValInC ?z"))), new BindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Float celcius = Float.valueOf(b.get(new Variable("?y")).getValue());
							bindings.append("?z:").append(convert(celcius)).append(",?x:").
								append(b.get(new Variable("?x")).getValue()).append("|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());

						CompletableFuture<BindingSet> future = new CompletableFuture<>();
						future.complete(bindingSet);
						return future;
					}

					public float convert(float fahrenheit) {
						return ((fahrenheit - 32) * 5) / 9;
					}

				}));
		 
		 
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?u hasValInF ?i"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?u hasValInC ?z"))), new BindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Float kelvin = Float.valueOf(b.get(new Variable("?i")).getValue());
							bindings.append("?z:").append(convert(kelvin)).append(",?x:")
							.append(b.get(new Variable("?u")).getValue()).
								append("|");
						}
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());
						
						CompletableFuture<BindingSet> future = new CompletableFuture<>();
						future.complete(bindingSet);
						return future;
					}

					public float convert(float kelvin) {
						return kelvin - 273;
					}
				}
		));
		
		
		 
		reasoner.addRule(new Rule(new HashSet<>(Arrays.asList(new TriplePattern("?u hasValInF ?i"))),
				new HashSet<>(Arrays.asList(new TriplePattern("?u hasValInF ?z"))), new BindingSetHandler() {

					@Override
					public CompletableFuture<BindingSet> handle(BindingSet bs) {
						
						StringBuilder bindings = new StringBuilder();
						for (Binding b : bs) {
							Float kelvin = Float.valueOf(b.get(new Variable("?i")).getValue());
							bindings.append("?z:").append(convertK(kelvin)).append(",?u:").
								append(b.get(new Variable("?u")).getValue()).append("|");
						}
						
						BindingSet bindingSet = Util.toBindingSet(bindings.toString());
						
						CompletableFuture<BindingSet> future = new CompletableFuture<>();
						future.complete(bindingSet);
						return future;

                    }


					public float convertK(float kelvin) {
						return ((kelvin * 9) / 5) - 459;
					}
				}
		));
		
		
		 
		reasoner.addRule(new Rule(new HashSet<>(),
				new HashSet<>(Arrays.asList(new TriplePattern("?i type Sensor"), new TriplePattern("?i inRoom ?j"))),
				new DataBindingSetHandler(new Table(new String[] {
				//@formatter:off
						"?i", "?j"
						//@formatter:on
				}, new String[] {
				//@formatter:off
						"<sensor3>,<kitchen>",
						"<sensor4>,<livingroom>",
						"<sensor1>,<bathroom>",
						"<sensor2>,<bedroom>",
						//@formatter:on
				}))));
		
	}


	@Test
	public void testRequestNonExistingData() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Device"));
		objective.add(new TriplePattern("?p hasValInC ?q"));

		// Start reasoning
		TaskBoard taskboard = new TaskBoard();
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		binding2.put("?q", "21");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertTrue(isEmpty(bind));

	}

	private boolean isEmpty(BindingSet b) {
		if (b.isEmpty() || b.iterator().next().isEmpty())
			return true;

		return false;
	}

	@Test
	public void testConverter() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Sensor"));
		objective.add(new TriplePattern("?p hasValInC ?q"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
//		binding2.put("?p", "<sensor1>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
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

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("?p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}

		System.out.println("bindings: " + bind);
		assertFalse(bind.isEmpty());

	}

	@Test
	public void testMoreThanOneInputBinding2() {
		// Formulate objective
		Binding b = new Binding();
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p hasValInC ?q"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding = new Binding();
		binding.put("?p", "<sensor2>");
		bs.add(binding);

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
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

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
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
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
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

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ONLY_BIGGEST_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();

		Binding binding2 = new Binding();
		binding2.put("?p", "<sensor1>");
		binding2.put("?q", "22");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
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

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
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
		objective.add(new TriplePattern("?p ?pred 22"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		// empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty()); // TODO THIS ONE SHOULD CONTAIN ONLY sensor1
	}

	/** 
	@Test
	public void testSendResultsFromOtherChildrenToNextChildren() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?p type Sensor"));
		objective.add(new TriplePattern("?p hasValInC ?q"));
		objective.add(new TriplePattern("?p inRoom ?r"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		// bindings
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		binding2.put("?r", "<livingroom>");
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}
	*/

	@Test
	public void testAllTriples() {
		// Formulate objective
		Set<TriplePattern> objective = new HashSet<>();
		objective.add(new TriplePattern("?s ?p ?o"));

		TaskBoard taskboard = new TaskBoard();

		// Start reasoning
		ReasoningNode root = reasoner.backwardPlan(objective, MatchStrategy.FIND_ALL_MATCHES, taskboard);
		System.out.println(root);

		// empty binding is necessary
		BindingSet bs = new BindingSet();
		Binding binding2 = new Binding();
		bs.add(binding2);

		BindingSet bind;
		while ((bind = root.continueBackward(bs)) == null) {
			System.out.println(root);
			taskboard.executeScheduledTasks();
		}
		System.out.println(root);

		System.out.println("bindings: " + bind);
		assertTrue(!bind.isEmpty());
	}
}
