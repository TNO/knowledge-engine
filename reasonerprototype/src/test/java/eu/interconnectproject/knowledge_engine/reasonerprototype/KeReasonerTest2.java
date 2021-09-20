package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Binding;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Variable;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Util;

public class KeReasonerTest2 {

	private KeReasonerAlt reasoner;

	@Before
	public void init() throws URISyntaxException {
		// Initialize
		reasoner = new KeReasonerAlt();
		reasoner.addRule(new RuleAlt(new HashSet<>(),
				new HashSet<>(Arrays.asList(new Triple("?a type Sensor"), new Triple("?a hasValInC ?b"))),
				new BindingSetHandler() {

					@Override
					public BindingSet handle(BindingSet bs) {
						BindingSet bindingSet = Util.toBindingSet("?a:<sensor1>,?b:22|?a:<sensor2>,?b:21");
						return bindingSet;
					}

				}));

		reasoner.addRule(new RuleAlt(new HashSet<>(),
				new HashSet<>(Arrays.asList(new Triple("?e type Sensor"), new Triple("?e hasValInF ?f"))),
				new BindingSetHandler() {

					@Override
					public BindingSet handle(BindingSet bs) {
						BindingSet bindingSet = Util.toBindingSet("?a:<sensor3>,?b:69|?a:<sensor4>,?b:71");
						return bindingSet;
					}
				}));

		reasoner.addRule(new RuleAlt(new HashSet<>(Arrays.asList(new Triple("?x hasValInF ?y"))),
				new HashSet<>(Arrays.asList(new Triple("?x hasValInC ?z"))), new BindingSetHandler() {

					@Override
					public BindingSet handle(BindingSet bs) {
						String bindings = "";
						for (Binding b : bs) {
							Float celcius = Float.valueOf(b.get(new Variable("?y")).getValue());
							bindings += "?y:" + convert(celcius) + ",|?x:" + b.get(new Variable("?x")).getValue();
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
	public void testConverter() {
		// Formulate objective
		Binding b = new Binding();
		Set<Triple> objective = new HashSet<>();
		objective.add(new Triple("?p type Sensor"));
		objective.add(new Triple("?p hasValInC ?q"));

		// Start reasoning
		NodeAlt root = reasoner.plan(objective);

		System.out.println(root);

		BindingSet bind = root.reason(new BindingSet());

		System.out.println(bind);

		System.out.println(TaskBoard.instance().tasks);

	}

}
