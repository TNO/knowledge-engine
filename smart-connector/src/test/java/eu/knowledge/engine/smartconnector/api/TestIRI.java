package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.junit.jupiter.api.Test;

import eu.knowledge.engine.smartconnector.api.GraphPattern;

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

	@Test
	void testGraphPattern() {
		new GraphPattern("?siteList a <http://www.w3.org/1999/02/22-rdf-syntax-ns#List> .\n"
				+ "    ?siteList <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?site .\n"
				+ "    ?site <https://whirpool.com/hasCode> ?siteCode .\n"
				+ "    ?site <https://saref.etsi.org/saref4bldg/contains> ?appList .\n"
				+ "    ?appList a <http://www.w3.org/1999/02/22-rdf-syntax-ns#List> .\n"
				+ "    ?appList <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?app . \n"
				+ "    ?app <https://whirpool.com/hasCode> ?appCode .\n"
				+ "    ?app <https://whirpool.com/hasDevType> ?appDevType .\n"
				+ "    ?app <http://xmlns.com/foaf/0.1#name> ?appName .\n"
				+ "    ?app <https://saref.etsi.org/core/hasState> ?appState .");
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
