package eu.knowledge.engine.reasoner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.JenaRDFSRulesTest;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.JenaRules;
import eu.knowledge.engine.reasoner.util.Table;

public class JenaRuleConversionTest {

	/**
	 * <a href="https://www.w3.org/TR/turtle/#sec-intro">source</a>
	 * 
	 * Removed Russian literal with language tag, because language tags are not
	 * supported by Apache Jena Rules syntax. Created feature request
	 * <a href="https://github.com/apache/jena/issues/3042">here</a>.
	 * 
	 */
	private static String turtleSource = """
			@base <http://example.org/> .
			@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
			@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
			@prefix foaf: <http://xmlns.com/foaf/0.1/> .
			@prefix rel: <http://www.perceive.net/schemas/relationship/> .

			<#green-goblin>
			    rel:enemyOf <#spiderman> ;
			    a foaf:Person ;    # in the context of the Marvel universe
			    foaf:name "Green Goblin" .

			<#spiderman>
			    rel:enemyOf <#green-goblin> ;
			    a foaf:Person ;
			    foaf:name "Spiderman" .
			""";

	@Test
	public void testTurtleToKERule() {
		StringReader sr = new StringReader(turtleSource);

		Set<BaseRule> rules = JenaRules.createFactRulesFromTurtle(sr);

		Rule r = (Rule) rules.iterator().next();

		Table t = ((DataBindingSetHandler) r.getBindingSetHandler()).getTable();

		Set<Map<String, String>> data = t.getData();

		assertEquals(6, data.size());

		for (Map<String, String> binding : data) {
			System.out.println(binding.get("s") + " " + binding.get("p") + " " + binding.get("o"));
		}

	}

	@Test
	public void testTurtleToJenaRules() {
		StringReader sr = new StringReader(turtleSource);

		String ruleString = JenaRules.createApacheJenaRulesFromTurtle(sr);

		System.out.println(ruleString);

		List<org.apache.jena.reasoner.rulesys.Rule> jenaRules = org.apache.jena.reasoner.rulesys.Rule
				.parseRules(ruleString);

		assertEquals(6, jenaRules.size());
	}

	@Test
	public void testJenaRulesToKeRules() {
		Set<BaseRule> rdfsRules = JenaRules.convertJenaToKeRules(JenaRDFSRulesTest.readRuleFile());

		assertEquals(12, rdfsRules.size());
	}

	/**
	 * Test body-less rule with multiple heads.
	 */
	@Test
	public void testJenaRulesToKeRulesMultipleFactsPerRule() {

		String ruleSource = """
				-> (rdf:type rdfs:range rdfs:Class) (rdfs:Resource rdf:type rdfs:Class).
				""";

		Set<BaseRule> rdfsRules = JenaRules.convertJenaToKeRules(ruleSource);
		assertEquals(1, rdfsRules.size());

		Rule r = (Rule) rdfsRules.iterator().next();
		DataBindingSetHandler handler = (DataBindingSetHandler) r.getBindingSetHandler();

		assertEquals(2, handler.getTable().getData().size());

	}

	/**
	 * Test empty rule string.
	 */
	@Test
	public void testJenaRulesToKeRulesEmptyString() {

		// without newline
		String ruleSource = "";

		Set<BaseRule> rdfsRules = JenaRules.convertJenaToKeRules(ruleSource);
		assertEquals(0, rdfsRules.size());

		// with newline
		ruleSource = """

				""";

		rdfsRules = JenaRules.convertJenaToKeRules(ruleSource);
		assertEquals(0, rdfsRules.size());
	}

	/**
	 * Test loading backward rule.
	 */
	@Test
	public void testJenaRulesToKeRulesBackward() {

		// without newline
		String ruleSource = """
				(?s ?p ?o) <- (?s ?p ?o) .
				""";

		Set<BaseRule> rdfsRules = JenaRules.convertJenaToKeRules(ruleSource);
		assertEquals(1, rdfsRules.size());
	}

	/**
	 * Test exception when loading rule with builtin.
	 */
	@Test
	public void testJenaRulesToKeRulesBuiltin() {

		// with unsupported builtin
		String ruleSource = """
				[max1: (?s rdf:type <http://www.example.org/Sensor>) -> print(?s) ]
				""";

		assertThrows(IllegalArgumentException.class, () -> {
			JenaRules.convertJenaToKeRules(ruleSource);
		});

	}

}
