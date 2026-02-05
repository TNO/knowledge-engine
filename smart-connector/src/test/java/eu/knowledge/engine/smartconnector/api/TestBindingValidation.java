package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;

public class TestBindingValidation {

	@Test
	void testIncomingOutgoingAnswerValid1() {
		var validator = new BindingValidator();
		var gp = new GraphPattern("?a ?b ?c .");

		var incoming = new BindingSet();
		var incoming1 = new Binding();
		incoming1.put("a", "\"something\"");
		incoming.add(incoming1);

		var outgoing = new BindingSet();
		var outgoing1 = new Binding();
		outgoing1.put("a", "\"something\"");
		outgoing.add(outgoing1);

		validator.validateIncomingOutgoingAnswer(gp, incoming, outgoing);
	}

	@Test
	void testMissingVariables() {
		assertThrows(IllegalArgumentException.class, () -> {
			var validator = new BindingValidator();
			var gp = new GraphPattern("?s ?p ?o");

			var bindingset = new BindingSet();
			var binding = new Binding();
			binding.put("s", "<s>");
			binding.put("o", "<o>");
			bindingset.add(binding);
			validator.validateCompleteBindings(gp, bindingset);
		});
	}

	@Test
	void testIncomingOutgoingAnswerValid2() {
		var validator = new BindingValidator();
		var gp = new GraphPattern("?a ?b ?c .");

		var incoming = new BindingSet();
		var incoming1 = new Binding();
		incoming1.put("a", "\"something 1\"");
		incoming.add(incoming1);
		var incoming2 = new Binding();
		incoming2.put("a", "\"something 2\"");
		incoming.add(incoming2);

		var outgoing = new BindingSet();
		var outgoing1 = new Binding();
		outgoing1.put("a", "\"something 1\"");
		outgoing.add(outgoing1);
		var outgoing2 = new Binding();
		outgoing2.put("a", "\"something 2\"");
		outgoing.add(outgoing2);

		validator.validateIncomingOutgoingAnswer(gp, incoming, outgoing);
	}

	@Test
	void testIncomingOutgoingAnswerInvalid1() {
		var validator = new BindingValidator();
		var gp = new GraphPattern("?a ?b ?c .");

		var incoming = new BindingSet();
		var incoming1 = new Binding();
		incoming1.put("a", "\"something\"");
		incoming.add(incoming1);

		var outgoing = new BindingSet();
		var outgoing1 = new Binding();
		outgoing1.put("a", "\"something else!\"");
		outgoing.add(outgoing1);

		assertThrows(IllegalArgumentException.class, () -> {
			validator.validateIncomingOutgoingAnswer(gp, incoming, outgoing);
		});
	}

	@Test
	void testIncomingOutgoingReactValid() {
		var validator = new BindingValidator();
		var argument = new GraphPattern("?a ?b ?c .");
		var result = new GraphPattern("?c ?d ?e .");

		var incoming = new BindingSet();
		var incoming1 = new Binding();
		incoming1.put("a", "\"something a\"");
		incoming1.put("b", "\"something b\"");
		incoming1.put("c", "\"something c\"");
		incoming.add(incoming1);

		var outgoing = new BindingSet();
		var outgoing1 = new Binding();
		outgoing1.put("c", "\"something c\"");
		outgoing1.put("d", "\"something d\"");
		outgoing1.put("e", "\"something e\"");
		outgoing.add(outgoing1);

		validator.validateIncomingOutgoingReact(argument, result, incoming, outgoing);
	}
}
