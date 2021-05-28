package eu.interconnectproject.knowledge_engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.junit.jupiter.api.Test;

class TestIRI {

	private String[] validUris = new String[] {
			//@formatter:off
			"https://asdlkasld.com",
			"mailto:hello",
			"https://hello",
			//@formatter:on
	};

	private String[] invalidUris = new String[] {
			//@formatter:off
			"",
			"https://",
			"strange characters and spaces | & ^",
			"/relative.nl"
			//@formatter:on
	};

	@Test
	void test() {
		for (String validUri : validUris) {
			System.out.println("valid uri: " + validUri);
			assertTrue(isValid(validUri));
		}

		for (String invalidUri : invalidUris) {
			System.out.println("invalid uri: " + invalidUri);
			assertFalse(isValid(invalidUri));
		}

	}

	public boolean isValid(String iri) {
		IRIFactory factory = IRIFactory.iriImplementation();

		try {
			IRI i = factory.construct(iri);
			if (i.isAbsolute()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

}
