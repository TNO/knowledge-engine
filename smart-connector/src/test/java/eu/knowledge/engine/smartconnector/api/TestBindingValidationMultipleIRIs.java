package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestBindingValidationMultipleIRIs {
	
	@Test
	void testBindingTwoIrisSeparatedWithSemicolon() {
		assertThrows(IllegalArgumentException.class, () -> {
			var b = new BindingValidator();
			b.validateValidBindingValue("<http://example.org/1>;<http://example.org/2>");
		});
	}

	@Test
	void testBindingTwoIrisSeparatedWithComma() {
		assertThrows(IllegalArgumentException.class, () -> {
			var b = new BindingValidator();
			b.validateValidBindingValue("<http://example.org/1>,<http://example.org/2>");
		});
	}

	@Test
	void testBindingTwoIrisSeparatedWithDot() {
		assertThrows(IllegalArgumentException.class, () -> {
			var b = new BindingValidator();
			b.validateValidBindingValue("<http://example.org/1>.<http://example.org/2>");
		});
	}
}
