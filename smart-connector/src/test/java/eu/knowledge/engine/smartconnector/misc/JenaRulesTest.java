package eu.knowledge.engine.smartconnector.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.util.DataBindingSetHandler;
import eu.knowledge.engine.reasoner.util.Table;
import eu.knowledge.engine.smartconnector.util.JenaRules;

public class JenaRulesTest {

	@Test
	public void test() {

		// source: https://www.w3.org/TR/turtle/#sec-intro
		String s = """
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
				    foaf:name "Spiderman", "Человек-паук"@ru .
				""";

		StringReader sr = new StringReader(s);

		Set<Rule> rules = JenaRules.createFactRulesFromTurtle(sr);

		Rule r = rules.iterator().next();

		Table t = ((DataBindingSetHandler) r.getBindingSetHandler()).getTable();

		Set<Map<String, String>> data = t.getData();

		assertEquals(7, data.size());

		for (Map<String, String> binding : data) {
			System.out.println(binding.get("s") + " " + binding.get("p") + " " + binding.get("o"));
		}

	}

}
